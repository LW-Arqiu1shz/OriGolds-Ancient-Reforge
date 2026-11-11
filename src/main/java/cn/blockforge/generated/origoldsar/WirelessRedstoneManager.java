package cn.blockforge.generated.origoldsar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 无线红石的运行时核心。
 *
 * <p>维护一张“虚拟电源表”：{@code 维度 -> (BlockPos#asLong -> 信号强度)}。
 * {@link cn.blockforge.generated.origoldsar.mixin.VirtualSignalMixin} 在原版取电路径
 * （{@code BlockState.getSignal / getDirectSignal}）的入口读取这张表，使任意位置的方块
 * 表现得像一个红石电源（弱信号 + 强信号，覆盖红石粉、中继器、比较器、活塞、灯等全部
 * 取电方式）。
 *
 * <p>每 tick（{@code ServerTickEvent.Post}）根据线路与发射器的期望状态重算这张表，
 * 对变化的位置调用 {@code level.updateNeighborsAt(...)}，让邻居按原版方式重新取电。
 */
public final class WirelessRedstoneManager {
    private WirelessRedstoneManager() {}

    /** 连接装置三类操作（连电线 / 删电线 / 反转电线）的统一交互射程（格）。 */
    public static final double INTERACT_RANGE = 32.0;

    /** 已应用的虚拟电源（仅服务器线程写入，Mixin 读取）。 */
    private static final Map<ResourceKey<Level>, Map<Long, Integer>> APPLIED = new ConcurrentHashMap<>();
    /** 连接装置的两个坐标记录点（起点 / 终点，顺序不限，两点齐了建立线路）。 */
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
    /**
     * “正在读取线路源信号”标记（线程局部）。为 {@code true} 时
     * {@link cn.blockforge.generated.origoldsar.mixin.VirtualSignalMixin} 屏蔽虚拟电源注入。
     * <p>线路源应只采样原版红石：读取源方块“邻居给它的输入”时，若看到隔壁就是一个
     * 虚拟输出（无线线路的终点 / 遥控开关），会把不该带电的邻居误当成被供电——这正是
     * 幽灵供电、自锁、以及“某个相邻区块的方块莫名其妙带电”的根源。用这个标记把邻居
     * 侧的虚拟输出从采样里剔除，同时保留源方块“自身作为电源的输出”（允许一条无线线路
     * 的输出作为另一条线路的源，即无线联级）。
     */
    private static final ThreadLocal<Boolean> READING_SOURCE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private record Pending(ResourceKey<Level> dimA, BlockPos posA, ResourceKey<Level> dimB, BlockPos posB) {
        static final Pending EMPTY = new Pending(null, null, null, null);

        Pending withA(ResourceKey<Level> dim, BlockPos pos) {
            return new Pending(dim, pos, dimB, posB);
        }

        Pending withB(ResourceKey<Level> dim, BlockPos pos) {
            return new Pending(dimA, posA, dim, pos);
        }

        Pending with(boolean start, ResourceKey<Level> dim, BlockPos pos) {
            return start ? withA(dim, pos) : withB(dim, pos);
        }
    }

    // ---------------------------------------------------------------- mixin 入口

    /** Mixin 调用：查询某个位置的虚拟电源强度，无则返回 0。 */
    public static int peek(ResourceKey<Level> dim, long packedPos) {
        Map<Long, Integer> map = APPLIED.get(dim);
        if (map == null) {
            return 0;
        }
        Integer v = map.get(packedPos);
        return v == null ? 0 : v;
    }

    /** Mixin 调用：是否正在“读取线路源信号”（此时应屏蔽虚拟电源注入）。 */
    public static boolean isReadingSource() {
        return READING_SOURCE.get();
    }

    /**
     * 读取某方块位置当前可用的红石信号：邻居的弱/强输入 + 它自身作为电源的输出。
     *
     * <p>“邻居的输入”两处（{@code getBestNeighborSignal} 与邻居的 {@code getDirectSignal}）
     * 会临时屏蔽虚拟电源注入：线路源必须采样原版红石，若附近就有一个虚拟输出（线路终点 /
     * 遥控开关），不屏蔽会把相邻方块误当成被供电，导致幽灵供电、自锁、或让不该带电的
     * 相邻区块方块带电。源方块“自身作为电源的输出”（{@code state.getSignal}）不清空，
     * 因此一个虚拟输出仍可作为另一条线路的源（无线联级）。
     */
    public static int readSignal(ServerLevel level, BlockPos pos) {
        // 源方块及其六个邻居所在的区块必须都已加载：getBestNeighborSignal 会读邻居方块，
        // 邻居若在一个未加载的区块里，ServerLevel 会当场同步加载（甚至生成）那个区块——
        // 线路源恰好在已加载区域边缘时就是每 tick 一次的主线程卡顿。没加载就当作没信号。
        if (!isNeighborhoodLoaded(level, pos)) {
            return 0;
        }
        boolean prev = READING_SOURCE.get();
        int sig;
        READING_SOURCE.set(Boolean.TRUE);
        try {
            sig = level.getBestNeighborSignal(pos);
        } finally {
            READING_SOURCE.set(prev);
        }
        BlockState state = level.getBlockState(pos);
        // 源方块自身作为电源的输出（不清空虚拟电源，允许无线联级）。
        for (Direction direction : Direction.values()) {
            sig = Math.max(sig, state.getSignal(level, pos, direction));
        }
        // 邻居直接供电（强信号）同样屏蔽虚拟电源，理由同上。
        READING_SOURCE.set(Boolean.TRUE);
        try {
            for (Direction direction : Direction.values()) {
                sig = Math.max(sig, level.getDirectSignal(pos.relative(direction), direction.getOpposite()));
            }
        } finally {
            READING_SOURCE.set(prev);
        }
        return sig;
    }

    // ---------------------------------------------------------------- 每 tick 同步

    public static void tick(MinecraftServer server) {
        WirelessRedstoneData data = WirelessRedstoneData.get(server);
        // 没有任何线路 / 发射器、也没有残留的虚拟电源时，本 tick 无事可做（绝大多数存档的常态）。
        if (data.links().isEmpty() && data.emitters().isEmpty() && !hasAppliedSignals()) {
            return;
        }

        // 汇总期望状态：dim -> (pos -> 强度)。同一点取最大值。
        Map<ResourceKey<Level>, Map<Long, Integer>> desired = new HashMap<>();
        for (WirelessRedstoneData.WirelessLink link : data.links()) {
            ServerLevel bLevel = server.getLevel(link.dimB());
            if (bLevel == null) {
                continue;
            }
            int sig = 0;
            ServerLevel aLevel = server.getLevel(link.dimA());
            if (aLevel != null) {
                sig = readSignal(aLevel, link.posA());
            }
            if (sig > 0) {
                desired.computeIfAbsent(link.dimB(), k -> new HashMap<>()).merge(link.posB().asLong(), sig, Math::max);
            }
        }
        for (WirelessRedstoneData.SwitchEmitter emitter : data.emitters()) {
            if (!emitter.active() || server.getLevel(emitter.dim()) == null) {
                continue;
            }
            desired.computeIfAbsent(emitter.dim(), k -> new HashMap<>()).merge(emitter.pos().asLong(), 15, Math::max);
        }

        Set<ResourceKey<Level>> dims = new HashSet<>();
        dims.addAll(APPLIED.keySet());
        dims.addAll(desired.keySet());
        for (ResourceKey<Level> dim : dims) {
            ServerLevel level = server.getLevel(dim);
            Map<Long, Integer> cur = APPLIED.computeIfAbsent(dim, k -> new HashMap<>());
            Map<Long, Integer> want = desired.getOrDefault(dim, Map.of());
            // 删除或清零。输出点周围的区块没加载齐时先不动它（保持上一次的值），下个 tick 再试：
            // 通知邻居会读写邻居方块，邻居在未加载区块里就会被同步加载。
            Iterator<Map.Entry<Long, Integer>> it = cur.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Integer> en = it.next();
                Integer w = want.get(en.getKey());
                if (w == null || w == 0) {
                    if (level != null && !isNeighborhoodLoaded(level, en.getKey())) {
                        continue;
                    }
                    it.remove();
                    if (level != null) {
                        notifyNeighbors(level, en.getKey());
                    }
                } else if (!w.equals(en.getValue())) {
                    if (level != null && !isNeighborhoodLoaded(level, en.getKey())) {
                        continue;
                    }
                    en.setValue(w);
                    if (level != null) {
                        notifyNeighbors(level, en.getKey());
                    }
                }
            }
            // 新增
            for (Map.Entry<Long, Integer> en : want.entrySet()) {
                if (!cur.containsKey(en.getKey())) {
                    if (level != null && !isNeighborhoodLoaded(level, en.getKey())) {
                        continue;
                    }
                    cur.put(en.getKey(), en.getValue());
                    if (level != null) {
                        notifyNeighbors(level, en.getKey());
                    }
                }
            }
        }
    }

    private static boolean hasAppliedSignals() {
        for (Map<Long, Integer> map : APPLIED.values()) {
            if (!map.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** 方块本身与六个邻居所在的区块是否都已加载（只查询，不会触发加载）。 */
    private static boolean isNeighborhoodLoaded(ServerLevel level, BlockPos pos) {
        return level.hasChunksAt(pos.getX() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getZ() + 1);
    }

    private static boolean isNeighborhoodLoaded(ServerLevel level, long packedPos) {
        return isNeighborhoodLoaded(level, WirelessRedstoneData.fromLong(packedPos));
    }

    /** 通知虚拟电源位置的六个邻居“这里的信号变了”，让邻居重新取电。 */
    private static void notifyNeighbors(ServerLevel level, long packedPos) {
        BlockPos pos = WirelessRedstoneData.fromLong(packedPos);
        if (!isNeighborhoodLoaded(level, pos)) {
            return;
        }
        level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
    }

    // ---------------------------------------------------------------- 服务端动作

    /** 连接装置：潜行 + 左键记录起点、潜行 + 右键记录终点（均来自客户端 32 格射线命中）。 */
    public static void onSelect(ServerPlayer player, long packedPos, boolean start) {
        recordPoint(player, packedPos, start);
    }

    /** 连接装置：潜行 + 右键记录终点坐标（由 {@link WirelessConnectorItem#useOn} 兜底调用）。 */
    public static void onUseOn(ServerPlayer player, BlockPos bPos) {
        recordPoint(player, bPos.asLong(), false);
    }

    /**
     * 两个坐标记录点：潜行左键记起点、潜行右键记终点，顺序不限；
     * 同一侧再次点击会覆盖该侧的坐标点，两侧都记录完成后自动建立线路并清空记录。
     * <p>只标了一个点时，对着已标好的那个方块再次按下“同一条边的键”（起点按潜行+左键、
     * 终点按潜行+右键）可取消该标记。
     */
    private static void recordPoint(ServerPlayer player, long packedPos, boolean start) {
        if (!holds(player, GeneratedMod.WIRELESS_CONNECTOR.get())) {
            return;
        }
        BlockPos pos = WirelessRedstoneData.fromLong(packedPos);
        Level level = player.level();
        // 校验失败也要给出可见提示，绝不能静默吞掉（否则玩家体感就是"按了没反应"）。
        if (!level.isLoaded(pos) || !isWithinInteractRange(player, pos)) {
            tell(player, "message.origolds_ar.wc_out_of_range");
            return;
        }
        if (level.getBlockState(pos).is(Blocks.REDSTONE_WIRE)) {
            tell(player, "message.origolds_ar.wc_invalid_wire");
            return;
        }
        UUID id = player.getUUID();
        Pending pending = PENDING.getOrDefault(id, Pending.EMPTY);

        // 只标了一个点、且对着那个已标记方块再次按“同一条边的键”时取消该侧标记。
        // 1.2.0 起取消只清对应的一边：另一端已经标好的点保留，不用重新选。
        // 同时校验维度：别在别的维度里“误取消”另一个维度已标的点（跨维度坐标可能巧合相同）。
        boolean onlyA = pending.posB() == null;
        boolean onlyB = pending.posA() == null;
        boolean cancelA = start && onlyA && pending.posA() != null
                && level.dimension().equals(pending.dimA()) && pending.posA().equals(pos);
        boolean cancelB = !start && onlyB && pending.posB() != null
                && level.dimension().equals(pending.dimB()) && pending.posB().equals(pos);
        if (cancelA || cancelB) {
            Pending cleared = cancelA ? pending.withA(null, null) : pending.withB(null, null);
            if (cleared.posA() == null && cleared.posB() == null) {
                PENDING.remove(id);
            } else {
                PENDING.put(id, cleared);
            }
            tell(player, "message.origolds_ar.wc_canceled");
            syncSelection(player);
            player.swing(findHand(player, GeneratedMod.WIRELESS_CONNECTOR.get()), true);
            return;
        }
        // 1.2.0 起“两端同点”在落子前就拦下：本次要标的点若与对侧已标点完全相同，
        // 直接提示并保持记录不动（改点另一侧即可重试）。
        boolean sameAsOther = start
                ? pending.dimB() != null && pending.posB() != null
                        && level.dimension().equals(pending.dimB()) && pending.posB().equals(pos)
                : pending.dimA() != null && pending.posA() != null
                        && level.dimension().equals(pending.dimA()) && pending.posA().equals(pos);
        if (sameAsOther) {
            tell(player, "message.origolds_ar.wc_same_block");
            return;
        }

        Pending next = pending.with(start, level.dimension(), pos);
        if (next.posA() != null && next.posB() != null) {
            // 两端都齐：建线并清空记录。
            WirelessRedstoneData.WirelessLink link = new WirelessRedstoneData.WirelessLink(
                    UUID.randomUUID(), next.dimA(), next.posA(), next.dimB(), next.posB());
            WirelessRedstoneData.get(player.level().getServer()).addLink(link);
            WirelessHistory.recordLink(player.getUUID(),
                    new WirelessHistory.LinkOp(WirelessHistory.LinkKind.ADDED, link));
            PENDING.remove(id);
            tell(player, "message.origolds_ar.wc_link_created");
            syncAll(player.level().getServer());
            syncSelection(player);
        } else {
            PENDING.put(id, next);
            PlayerInfoBar.show(player, Component.translatable(start
                    ? "message.origolds_ar.wc_selected_a" : "message.origolds_ar.wc_selected_b",
                    pos.getX(), pos.getY(), pos.getZ()));
            syncSelection(player);
        }
        player.swing(findHand(player, GeneratedMod.WIRELESS_CONNECTOR.get()), true);
    }

    /** 连接装置：显示模式下按 Delete 删除悬停的线路（需在 32 格射程内）。 */
    public static void onDelete(ServerPlayer player, UUID id) {
        if (!holds(player, GeneratedMod.WIRELESS_CONNECTOR.get())) {
            return;
        }
        WirelessRedstoneData data = WirelessRedstoneData.get(player.level().getServer());
        WirelessRedstoneData.WirelessLink link = data.link(id);
        if (link != null && !withinRange(player, link)) {
            tell(player, "message.origolds_ar.wc_out_of_range");
            return;
        }
        if (data.removeLink(id)) {
            if (link != null) {
                WirelessHistory.recordLink(player.getUUID(),
                        new WirelessHistory.LinkOp(WirelessHistory.LinkKind.REMOVED, link));
            }
            tell(player, "message.origolds_ar.wc_link_removed");
            syncAll(player.level().getServer());
        }
    }

    /** 连接装置：显示模式下按 Backspace 反转悬停的线路方向（交换输入端/输出端，需在 32 格射程内）。 */
    public static void onReverse(ServerPlayer player, UUID id) {
        if (!holds(player, GeneratedMod.WIRELESS_CONNECTOR.get())) {
            return;
        }
        WirelessRedstoneData data = WirelessRedstoneData.get(player.level().getServer());
        WirelessRedstoneData.WirelessLink link = data.link(id);
        if (link == null) {
            return;
        }
        if (!withinRange(player, link)) {
            tell(player, "message.origolds_ar.wc_out_of_range");
            return;
        }
        // 反转后若与一条既有线路完全重合（反向已存在），拒绝，避免出现两条同点反向线路。
        if (data.hasLink(link.dimB(), link.posB(), link.dimA(), link.posA())) {
            tell(player, "message.origolds_ar.wc_reverse_exists");
            return;
        }
        if (data.reverseLink(id)) {
            // 反转是自身的逆操作：记录一条 REVERSED（快照为反转前的线路）。
            WirelessHistory.recordLink(player.getUUID(),
                    new WirelessHistory.LinkOp(WirelessHistory.LinkKind.REVERSED, link));
            tell(player, "message.origolds_ar.wc_link_reversed");
            syncAll(player.level().getServer());
        }
    }

    /** 遥控开关：潜行 + 左键绑定当前维度中的方块（统一 32 格射程，且必须命中真实方块而非空气）。 */
    public static void onBind(ServerPlayer player, long packedPos) {
        if (!holds(player, GeneratedMod.ORIGOLD_REMOTE_SWITCH.get())) {
            return;
        }
        BlockPos pos = WirelessRedstoneData.fromLong(packedPos);
        // 失败必须可见，否则玩家体感就是"按了没反应"。
        if (!player.level().isLoaded(pos) || !isWithinInteractRange(player, pos)) {
            tell(player, "message.origolds_ar.wc_out_of_range");
            return;
        }
        if (player.level().getBlockState(pos).isAir()) {
            tell(player, "message.origolds_ar.rs_invalid_target");
            return;
        }
        InteractionHand hand = findHand(player, GeneratedMod.ORIGOLD_REMOTE_SWITCH.get());
        ItemStack stack = player.getItemInHand(hand);
        // 撤销历史按“每一把遥控开关”独立保存：身份来自 uid 组件，首次记录操作时惰性分配。
        UUID switchUid = ensureSwitchUid(stack);
        RemoteSwitchBinding old = stack.get(GeneratedMod.REMOTE_SWITCH_BINDING.get());
        WirelessRedstoneData.SwitchEmitter oldEmitter =
                old == null ? null : WirelessRedstoneData.get(player.level().getServer()).emitter(old.emitter());
        if (old != null) {
            WirelessRedstoneData.get(player.level().getServer()).removeEmitter(old.emitter());
        }
        RemoteSwitchBinding binding = new RemoteSwitchBinding(
                player.level().dimension(), pos, UUID.randomUUID(), false);
        stack.set(GeneratedMod.REMOTE_SWITCH_BINDING.get(), binding);
        WirelessHistory.recordSwitch(switchUid, player.getUUID(), new WirelessHistory.SwitchOp(
                old, oldEmitter, binding, WirelessRedstoneData.get(player.level().getServer()).emitter(binding.emitter())));
        PlayerInfoBar.show(player, Component.translatable("message.origolds_ar.rs_bound",
                pos.getX(), pos.getY(), pos.getZ()));
        player.swing(hand, true);
    }

    /**
     * 遥控开关：准星对准已绑定的方块按下 Delete，解除该绑定并移除对应的远程信号发射器。
     * 需在当前维度、且命中的就是当前绑定的方块、并在统一 32 格射程内。
     */
    public static void onUnbind(ServerPlayer player, long packedPos) {
        if (!holds(player, GeneratedMod.ORIGOLD_REMOTE_SWITCH.get())) {
            return;
        }
        InteractionHand hand = findHand(player, GeneratedMod.ORIGOLD_REMOTE_SWITCH.get());
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.get(GeneratedMod.REMOTE_SWITCH_BINDING.get()) instanceof RemoteSwitchBinding binding)) {
            // 已经没绑定了：静默返回，避免按住 Delete 重复弹提示。
            return;
        }
        BlockPos target = WirelessRedstoneData.fromLong(packedPos);
        if (!binding.dimension().equals(player.level().dimension())
                || !binding.position().equals(target)
                || !isWithinInteractRange(player, target)) {
            tell(player, "message.origolds_ar.wc_out_of_range");
            return;
        }
        WirelessRedstoneData.SwitchEmitter oldEmitter =
                WirelessRedstoneData.get(player.level().getServer()).emitter(binding.emitter());
        WirelessRedstoneData.get(player.level().getServer()).removeEmitter(binding.emitter());
        stack.remove(GeneratedMod.REMOTE_SWITCH_BINDING.get());
        WirelessHistory.recordSwitch(ensureSwitchUid(stack), player.getUUID(),
                new WirelessHistory.SwitchOp(binding, oldEmitter, null, null));
        tell(player, "message.origolds_ar.rs_unbound_confirm");
        player.swing(hand, true);
    }

    /** 遥控开关：右键远程触发（由 {@link RemoteSwitchItem} 调用）。 */
    public static void onTrigger(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.get(GeneratedMod.REMOTE_SWITCH_BINDING.get()) instanceof RemoteSwitchBinding binding)) {
            tell(player, "message.origolds_ar.rs_unbound");
            return;
        }
        ServerLevel level = player.level().getServer().getLevel(binding.dimension());
        if (level == null || !level.isLoaded(binding.position())) {
            tell(player, "message.origolds_ar.rs_unloaded");
            return;
        }
        BlockState state = level.getBlockState(binding.position());
        if (state.getBlock() instanceof net.minecraft.world.level.block.LeverBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.ButtonBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.CopperBulbBlock) {
            // 可被玩家直接点击交互的红石方块：远程“按一次”。
            state.useWithoutItem(level, player,
                    new net.minecraft.world.phys.BlockHitResult(
                            net.minecraft.world.phys.Vec3.atCenterOf(binding.position()),
                            Direction.UP, binding.position(), false));
            tell(player, "message.origolds_ar.rs_interacted");
            player.swing(hand, true);
            return;
        }
        // 其它方块：像远程拉杆一样切换该位置的 15 级信号输出。
        WirelessRedstoneData data = WirelessRedstoneData.get(player.level().getServer());
        WirelessRedstoneData.SwitchEmitter emitter = data.emitter(binding.emitter());
        boolean turnOff = emitter != null && emitter.active();
        data.putEmitter(new WirelessRedstoneData.SwitchEmitter(
                binding.emitter(), binding.dimension(), binding.position(), !turnOff));
        stack.set(GeneratedMod.REMOTE_SWITCH_BINDING.get(),
                new RemoteSwitchBinding(binding.dimension(), binding.position(), binding.emitter(), !turnOff));
        tell(player, turnOff ? "message.origolds_ar.rs_signal_off" : "message.origolds_ar.rs_signal_on");
        player.swing(hand, true);
    }

    // ---------------------------------------------------------------- 撤销 / 重做

    /**
     * Ctrl+Z（撤销）/ Ctrl+Y（重做）：按当前手持选择历史栈，主手优先于副手——
     * 手持某一把遥控开关时，只撤销 / 重做<b>这一把</b>开关自己的绑定 / 解绑操作
     * （每一把开关独立保存历史）；手持任意连接装置时，撤销 / 重做无线线路相关操作
     * （新增 / 删除连线、反转输入输出方向）。两件都没拿：直接忽略。
     */
    public static void onUndoRedo(ServerPlayer player, boolean undo) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(GeneratedMod.ORIGOLD_REMOTE_SWITCH.get())) {
                undoSwitch(player, stack, undo);
                return;
            }
        }
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(GeneratedMod.WIRELESS_CONNECTOR.get())) {
                undoLink(player, undo);
                return;
            }
        }
    }

    /** 连接装置：撤销 / 重做一次线路操作（历史按玩家统一一支，手持任意连接装置通用）。 */
    private static void undoLink(ServerPlayer player, boolean undo) {
        WirelessHistory.Op op = undo ? WirelessHistory.undoLink(player.getUUID())
                : WirelessHistory.redoLink(player.getUUID());
        if (!(op instanceof WirelessHistory.LinkOp link)) {
            tell(player, undo ? "message.origolds_ar.undo_nothing" : "message.origolds_ar.redo_nothing");
            return;
        }
        WirelessRedstoneData data = WirelessRedstoneData.get(player.level().getServer());
        switch (link.kind()) {
            case ADDED -> {
                if (undo) {
                    data.removeLink(link.link().id());
                } else if (data.link(link.link().id()) == null) {
                    data.addLink(link.link());
                }
            }
            case REMOVED -> {
                if (undo) {
                    if (data.link(link.link().id()) == null) {
                        data.addLink(link.link());
                    }
                } else {
                    data.removeLink(link.link().id());
                }
            }
            // 反转是自身的逆操作：撤销与重做都再反一次。
            case REVERSED -> data.reverseLink(link.link().id());
        }
        tell(player, undo ? "message.origolds_ar.undo_done" : "message.origolds_ar.redo_done",
                WirelessHistory.describe(op));
        syncAll(player.level().getServer());
    }

    /** 遥控开关：撤销 / 重做这一把开关自己的绑定 / 解绑操作。 */
    private static void undoSwitch(ServerPlayer player, ItemStack stack, boolean undo) {
        UUID switchUid = stack.get(GeneratedMod.REMOTE_SWITCH_UID.get());
        WirelessHistory.Op op = switchUid == null ? null
                : (undo ? WirelessHistory.undoSwitch(switchUid) : WirelessHistory.redoSwitch(switchUid));
        if (!(op instanceof WirelessHistory.SwitchOp sw)) {
            tell(player, undo ? "message.origolds_ar.undo_nothing" : "message.origolds_ar.redo_nothing");
            return;
        }
        RemoteSwitchBinding target = undo ? sw.beforeBinding() : sw.afterBinding();
        WirelessRedstoneData.SwitchEmitter emitter = undo ? sw.beforeEmitter() : sw.afterEmitter();
        WirelessRedstoneData data = WirelessRedstoneData.get(player.level().getServer());
        // 先撤掉这把开关此刻挂在世界里的发射器，避免切换后留下幽灵电源。
        RemoteSwitchBinding current = stack.get(GeneratedMod.REMOTE_SWITCH_BINDING.get());
        if (current != null) {
            data.removeEmitter(current.emitter());
        }
        if (target != null) {
            stack.set(GeneratedMod.REMOTE_SWITCH_BINDING.get(), target);
        } else {
            stack.remove(GeneratedMod.REMOTE_SWITCH_BINDING.get());
        }
        if (emitter != null) {
            data.putEmitter(emitter);
        }
        tell(player, undo ? "message.origolds_ar.undo_done" : "message.origolds_ar.redo_done",
                WirelessHistory.describe(op));
    }

    /** 取（必要时惰性分配）这一把遥控开关的独立身份 id。 */
    private static UUID ensureSwitchUid(ItemStack stack) {
        UUID uid = stack.get(GeneratedMod.REMOTE_SWITCH_UID.get());
        if (uid == null) {
            uid = UUID.randomUUID();
            stack.set(GeneratedMod.REMOTE_SWITCH_UID.get(), uid);
        }
        return uid;
    }

    // ---------------------------------------------------------------- 同步与清理

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildSyncPayload(player.level().getServer()));
    }

    public static void syncAll(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(buildSyncPayload(server));
    }

    /** 把某个玩家当前的“待选中转点”同步给该玩家（已选一个点、还差第二个点时客户端画呼吸高亮框）。 */
    public static void syncSelection(ServerPlayer player) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null) {
            PacketDistributor.sendToPlayer(player, new WirelessSelectSyncPayload(false, null, 0L, false));
            return;
        }
        // 只同步真正“选了但还差另一个”的那一个点。
        if (pending.posA() != null) {
            PacketDistributor.sendToPlayer(player, new WirelessSelectSyncPayload(
                    true, pending.dimA(), pending.posA().asLong(), true));
        } else if (pending.posB() != null) {
            PacketDistributor.sendToPlayer(player, new WirelessSelectSyncPayload(
                    true, pending.dimB(), pending.posB().asLong(), false));
        } else {
            PacketDistributor.sendToPlayer(player, new WirelessSelectSyncPayload(false, null, 0L, false));
        }
    }

    /** 玩家眼睛到线路线段的最短距离是否在统一射程内（只对同维度线路生效）。 */
    private static boolean withinRange(ServerPlayer player, WirelessRedstoneData.WirelessLink link) {
        ResourceKey<Level> dim = player.level().dimension();
        if (!dim.equals(link.dimA()) || !dim.equals(link.dimB())) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 a = Vec3.atCenterOf(link.posA());
        Vec3 b = Vec3.atCenterOf(link.posB());
        return distSqToSegment(eye, a, b) <= INTERACT_RANGE * INTERACT_RANGE;
    }

    /** 点 p 到线段 ab 的最短距离平方。 */
    private static double distSqToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lenSq = ab.lengthSqr();
        if (lenSq < 1.0E-6) {
            return p.distanceToSqr(a);
        }
        double t = p.subtract(a).dot(ab) / lenSq;
        t = Math.max(0.0, Math.min(1.0, t));
        Vec3 c = a.add(ab.x * t, ab.y * t, ab.z * t);
        return p.distanceToSqr(c);
    }

    private static WirelessLinkSyncPayload buildSyncPayload(MinecraftServer server) {
        List<WirelessLinkSyncPayload.LinkView> views = new ArrayList<>();
        for (WirelessRedstoneData.WirelessLink link : WirelessRedstoneData.get(server).links()) {
            views.add(new WirelessLinkSyncPayload.LinkView(
                    link.id(), link.dimA(), link.posA().asLong(), link.dimB(), link.posB().asLong()));
        }
        return new WirelessLinkSyncPayload(views);
    }

    /** 服务器关闭 / 单机关档时清理运行时状态（含撤销/重做历史）。 */
    public static void clearRuntime() {
        APPLIED.clear();
        PENDING.clear();
        WirelessHistory.clearAll();
    }

    /** 维度卸载时清掉其虚拟电源，重新加载后下一 tick 会重新应用并通知邻居。 */
    public static void onLevelUnload(Level level) {
        APPLIED.remove(level.dimension());
    }

    public static boolean holds(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    private static InteractionHand findHand(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getMainHandItem().is(item) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static void tell(ServerPlayer player, String key) {
        // 1.2.0 起统一走玩家信息栏（基模不在时自动回退 action bar）。
        PlayerInfoBar.show(player, Component.translatable(key));
    }

    /** 带一个子键名参数的提示（“已撤销：新增连线”一类）。 */
    private static void tell(ServerPlayer player, String key, String nameKey) {
        PlayerInfoBar.show(player, Component.translatable(key, Component.translatable(nameKey)));
    }

    /** 26.1.2 移除了 Player#canInteractWithBlock，这里按眼睛位置到方块中心的距离判断。 */
    private static boolean isWithinInteractRange(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) {
        return player.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))
                <= INTERACT_RANGE * INTERACT_RANGE;
    }
}
