package cn.blockforge.generated.origoldsar.mixin;

import cn.blockforge.generated.origoldsar.WirelessRedstoneData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.RemoteSwitchBinding;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 把金源无线红石“线路”（{@link WirelessRedstoneData.WirelessLink}）随结构方块一起保存/加载。
 *
 * <p>连接装置与遥控开关都是手持物品，不落在世界里；真正在世界里的“无线电”只有那张
 * 虚拟线路表（保存在主世界 SavedData，键为绝对坐标）。因此结构方块复制工具并不能
 * 天然带走这些线路——<b>普通结构方块只保存方块与方块实体/实体，看不到我们的线路表</b>。
 *
 * <p>这个 mixin 在 {@code StructureTemplate} 上做四件事（线路与遥控开关两条管线）：
 * <ul>
 *   <li>{@code fillFromWorld}（结构方块保存时）：把“两个端点都落在被保存区域内”的线路
 *       记为核心区的<em>相对坐标</em>，固化到模板对象上；同时扫描被保存区域内的<b>物品展示框</b>，
 *       把其中“已绑定方块”的遥控开关（金源无线红石遥控开关）也记成相对坐标。</li>
 *   <li>{@code save}：把相对坐标写进模板 NBT（{@code origolds_ar_radio_links} 与
 *       {@code origolds_ar_remote_switches} 键），这样结构文件本身带上无线电数据，存档重载后依然可用；</li>
 *   <li>{@code load}：从模板 NBT 读回相对坐标；</li>
 *   <li>{@code placeInWorld}（摆放时）：把相对坐标按与方块相同的变换（旋转/镜像/平移）
 *       换算回世界坐标，并调用
 *       {@link WirelessRedstoneData#addLinkIfAbsent(WirelessLink)} 重建线路（同端点不重复）；
 *       对展示框里的遥控开关，按开关身份 id（{@code origolds_ar:remote_switch_uid} 组件）匹配
 *       摆放后的展示框，把它的绑定坐标重定位到新世界坐标，并重建成新位置的远程信号发射器。
 *   </li>
 * </ul>
 *
 * <p>只处理<b>两端都在区域内</b>的线路：结构方块是单维度、整体平移的，一端在外、一端在
 * 内的线路在移动后必然指向错误世界坐标，复制它反而添乱。展示框里的遥控开关则靠“随结构
 * 保存/摆放的物品组件 + 开关身份 id”定位：绑定坐标原本存的是<b>绝对坐标</b>，若不重定位，
 * 粘贴到别处后遥控还指着旧位置，因此这里把它仿照线路一样按相对坐标迁移。
 *
 * <p>保存/恢复只发生在服务端线程，且仅当模板自身带有机无线电数据（世界生成器里的
 * 原生结构模板没有该标签，天然为空、不动作）。恢复逻辑用 try/catch 包住：摆放方块是
 * 操作失败必须可见的原版行为，绝不能让无线线路恢复失败去中断整块结构的摆放。
 */
@Mixin(StructureTemplate.class)
public abstract class StructureTemplateRadioMixin {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RADIO_TAG = "origolds_ar_radio_links";
    private static final String REMOTE_TAG = "origolds_ar_remote_switches";

    /** 一条被捕获的线路（相对核心区左下的相对坐标）。 */
    @Unique
    private record SavedLink(BlockPos localA, BlockPos localB) {}

    /** 被捕获的遥控开关：身份 id、被绑定方块相对核心区的相对坐标、当前信号开/关。 */
    @Unique
    private record SavedRemote(UUID uid, BlockPos local, boolean active) {}

    @Unique
    private List<SavedLink> origoldsAr$radioLinks = List.of();

    @Unique
    private List<SavedRemote> origoldsAr$remoteSwitches = List.of();

    // ---------------------------------------------------------------- 保存：捕获

    /**
     * 结构方块保存时：结构填进模板后，把两端都落在保存区域内的线路记录为相对坐标。
     * 区域为 {@code [origin, origin + size - 1]}（{@code fillFromWorld} 内部已保证
     * origin 是左下角，因为 size 每轴都 >= 1）。
     */
    @Inject(method = "fillFromWorld", at = @At("RETURN"))
    private void origoldsAr$captureLinks(Level level, BlockPos origin, Vec3i size,
                                         boolean includeEntities, List<Block> ignored,
                                         CallbackInfo ci) {
        this.origoldsAr$radioLinks = List.of();
        this.origoldsAr$remoteSwitches = List.of();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        try {
            MinecraftServer server = serverLevel.getServer();
            WirelessRedstoneData data = WirelessRedstoneData.get(server);
            ResourceKey<Level> dim = level.dimension();
            BlockPos max = origin.offset(size).offset(-1, -1, -1);
            List<SavedLink> captured = new ArrayList<>();
            for (WirelessRedstoneData.WirelessLink link : data.links()) {
                if (!dim.equals(link.dimA()) || !dim.equals(link.dimB())) {
                    continue;
                }
                if (!within(origin, max, link.posA()) || !within(origin, max, link.posB())) {
                    continue;
                }
                captured.add(new SavedLink(link.posA().subtract(origin), link.posB().subtract(origin)));
            }
            this.origoldsAr$radioLinks = captured;
            this.origoldsAr$remoteSwitches = captureRemotes(serverLevel, origin, max, includeEntities);
        } catch (Exception e) {
            LOGGER.warn("[origolds_ar] 结构方块保存时捕获无线数据失败", e);
        }
    }

    /** 结构方块保存时（仅当勾选“保存实体”）：扫描区域内展示框，捕获已绑定的遥控开关。 */
    @Unique
    private static List<SavedRemote> captureRemotes(ServerLevel level, BlockPos origin, BlockPos max,
                                                    boolean includeEntities) {
        List<SavedRemote> remotes = new ArrayList<>();
        if (!includeEntities) {
            return remotes;
        }
        try {
            AABB area = new AABB(Vec3.atLowerCornerOf(origin), Vec3.atLowerCornerOf(max).add(1.0, 1.0, 1.0));
            for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, area, e -> true)) {
                ItemStack stack = frame.getItem();
                if (!stack.is(GeneratedMod.ORIGOLD_REMOTE_SWITCH.get())) {
                    continue;
                }
                if (!(stack.get(GeneratedMod.REMOTE_SWITCH_BINDING.get()) instanceof RemoteSwitchBinding binding)) {
                    continue;
                }
                UUID uid = stack.get(GeneratedMod.REMOTE_SWITCH_UID.get());
                if (uid == null) {
                    // 正常流程下绑定过的开关必有 uid（绑定/解绑时都会惰性分配）。
                    continue;
                }
                remotes.add(new SavedRemote(uid, binding.position().subtract(origin), binding.active()));
            }
        } catch (Exception e) {
            LOGGER.warn("[origolds_ar] 结构方块保存时捕获遥控开关失败", e);
        }
        return remotes;
    }

    // ---------------------------------------------------------------- 序列化

    @Inject(method = "save", at = @At("RETURN"))
    private void origoldsAr$writeLinks(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        try {
            if (!this.origoldsAr$radioLinks.isEmpty()) {
                ListTag list = new ListTag();
                for (SavedLink link : this.origoldsAr$radioLinks) {
                    CompoundTag entry = new CompoundTag();
                    entry.putLong("a", link.localA().asLong());
                    entry.putLong("b", link.localB().asLong());
                    list.add(entry);
                }
                tag.put(RADIO_TAG, list);
            }
            if (!this.origoldsAr$remoteSwitches.isEmpty()) {
                ListTag list = new ListTag();
                for (SavedRemote remote : this.origoldsAr$remoteSwitches) {
                    CompoundTag entry = new CompoundTag();
                    entry.putString("uid", remote.uid().toString()); // 26.1.2 起 CompoundTag 不再有 putUUID
                    entry.putLong("local", remote.local().asLong());
                    entry.putBoolean("active", remote.active());
                    list.add(entry);
                }
                tag.put(REMOTE_TAG, list);
            }
        } catch (Exception e) {
            LOGGER.warn("[origolds_ar] 结构方块写入无线数据失败", e);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void origoldsAr$readLinks(HolderGetter<?> holderGetter, CompoundTag tag, CallbackInfo ci) {
        this.origoldsAr$radioLinks = List.of();
        this.origoldsAr$remoteSwitches = List.of();
        try {
            if (tag.contains(RADIO_TAG)) {
                ListTag list = tag.getListOrEmpty(RADIO_TAG);
                List<SavedLink> links = new ArrayList<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag entry = list.getCompoundOrEmpty(i);
                    links.add(new SavedLink(
                            WirelessRedstoneData.fromLong(entry.getLongOr("a", 0L)),
                            WirelessRedstoneData.fromLong(entry.getLongOr("b", 0L))));
                }
                this.origoldsAr$radioLinks = links;
            }
            if (tag.contains(REMOTE_TAG)) {
                ListTag list = tag.getListOrEmpty(REMOTE_TAG);
                List<SavedRemote> remotes = new ArrayList<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag entry = list.getCompoundOrEmpty(i);
                    remotes.add(new SavedRemote(
                            // 宽松读取：26.x 写的是字符串，1.21.1 存下来的结构文件是 int 数组，两种都认，
                            // 否则老结构里的遥控开关全部匹配不上；格式不对也只丢这一条，不影响其它。
                            entry.read("uid", net.minecraft.core.UUIDUtil.LENIENT_CODEC).orElse(new java.util.UUID(0L, 0L)),
                            WirelessRedstoneData.fromLong(entry.getLongOr("local", 0L)),
                            entry.getBooleanOr("active", false)));
                }
                this.origoldsAr$remoteSwitches = remotes;
            }
        } catch (Exception e) {
            LOGGER.warn("[origolds_ar] 结构方块读取无线数据失败", e);
        }
    }

    // ---------------------------------------------------------------- 恢复

    /**
     * 结构方块摆放时：把捕获的相对坐标按与方块一致的位置变换（镜像/旋转/平移）换回
     * 世界坐标，再重建线路。位置变换与 {@code placeInWorld} 内部所用完全一致：
     * {@code world = StructureTemplate.calculateRelativePosition(settings, local).offset(pos)}。
     */
    @Inject(method = "placeInWorld", at = @At("RETURN"))
    private void origoldsAr$restoreLinks(ServerLevelAccessor level, BlockPos pos, BlockPos pivot,
                                         StructurePlaceSettings settings, RandomSource random, int flags,
                                         CallbackInfoReturnable<Boolean> cir) {
        // 只在确实摆放成功、且模板确实带无线电数据时恢复。
        if (!cir.getReturnValue() || (this.origoldsAr$radioLinks.isEmpty() && this.origoldsAr$remoteSwitches.isEmpty())) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        try {
            MinecraftServer server = serverLevel.getServer();
            WirelessRedstoneData data = WirelessRedstoneData.get(server);
            ResourceKey<Level> dim = serverLevel.dimension();
            boolean added = false;
            for (SavedLink link : this.origoldsAr$radioLinks) {
                BlockPos worldA = StructureTemplate.calculateRelativePosition(settings, link.localA()).offset(pos);
                BlockPos worldB = StructureTemplate.calculateRelativePosition(settings, link.localB()).offset(pos);
                added |= data.addLinkIfAbsent(new WirelessRedstoneData.WirelessLink(
                        UUID.randomUUID(), dim, worldA, dim, worldB));
            }
            // 新线路要广播给客户端，否则显示模式下的穿墙线渲染不会出现。
            if (added) {
                cn.blockforge.generated.origoldsar.WirelessRedstoneManager.syncAll(server);
            }
            restoreRemotes(serverLevel, pos, settings);
        } catch (Exception e) {
            LOGGER.warn("[origolds_ar] 结构方块摆放时恢复无线数据失败", e);
        }
    }

    /**
     * 结构方块摆放时：把展示框里已绑定的遥控开关按相对坐标重定位到新世界坐标。
     * 匹配依据是开关的独立身份 id（{@code origolds_ar:remote_switch_uid} 组件，随物品持久化）：
     * 扫描摆放区域内所有展示框，命中后更新其绑定坐标，并在新位置重建远程信号发射器。
     * 对拉杆/按钮/铜灯这类“可点交互”的红石方块不建发射器，避免误注入 15 级幽灵电源。
     */
    @Unique
    private void restoreRemotes(ServerLevel level, BlockPos pos, StructurePlaceSettings settings) {
        if (this.origoldsAr$remoteSwitches.isEmpty()) {
            return;
        }
        MinecraftServer server = level.getServer();
        WirelessRedstoneData data = WirelessRedstoneData.get(server);
        ResourceKey<Level> dim = level.dimension();
        Map<UUID, SavedRemote> byUid = new HashMap<>();
        for (SavedRemote remote : this.origoldsAr$remoteSwitches) {
            byUid.put(remote.uid(), remote);
        }
        AABB area = placedArea((StructureTemplate)(Object) this, settings, pos);
        for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, area, e -> true)) {
            ItemStack stack = frame.getItem();
            if (!stack.is(GeneratedMod.ORIGOLD_REMOTE_SWITCH.get())) {
                continue;
            }
            UUID uid = stack.get(GeneratedMod.REMOTE_SWITCH_UID.get());
            if (uid == null) {
                continue;
            }
            SavedRemote saved = byUid.get(uid);
            if (saved == null) {
                continue;
            }
            if (!(stack.get(GeneratedMod.REMOTE_SWITCH_BINDING.get()) instanceof RemoteSwitchBinding binding)) {
                continue;
            }
            BlockPos worldPos = StructureTemplate.calculateRelativePosition(settings, saved.local()).offset(pos);
            stack.set(GeneratedMod.REMOTE_SWITCH_BINDING.get(),
                    new RemoteSwitchBinding(dim, worldPos, binding.emitter(), saved.active()));
            frame.setItem(stack);
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(worldPos);
            boolean interactive = state.getBlock() instanceof net.minecraft.world.level.block.LeverBlock
                    || state.getBlock() instanceof net.minecraft.world.level.block.ButtonBlock
                    || state.getBlock() instanceof net.minecraft.world.level.block.CopperBulbBlock;
            if (!interactive) {
                data.putEmitter(new WirelessRedstoneData.SwitchEmitter(
                        binding.emitter(), dim, worldPos, saved.active()));
            }
        }
    }

    /** 由摆放设定（旋转/镜像/平移）推算整块结构在世界的包围盒，外扩一圈保证框到边界上的展示框。 */
    @Unique
    private static AABB placedArea(StructureTemplate template, StructurePlaceSettings settings, BlockPos pos) {
        Vec3i size = template.getSize();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        // 旋转 / 镜像都是保持轴对齐的线性变换，包围盒由 8 个角决定即可。
        // 以前逐格遍历整块结构（48³ 的结构要变换十几万次），摆放大结构时会明显卡一下。
        for (int dx : new int[] {0, size.getX()}) {
            for (int dy : new int[] {0, size.getY()}) {
                for (int dz : new int[] {0, size.getZ()}) {
                    BlockPos corner = StructureTemplate.calculateRelativePosition(settings, new BlockPos(dx, dy, dz)).offset(pos);
                    minX = Math.min(minX, corner.getX());
                    minY = Math.min(minY, corner.getY());
                    minZ = Math.min(minZ, corner.getZ());
                    maxX = Math.max(maxX, corner.getX());
                    maxY = Math.max(maxY, corner.getY());
                    maxZ = Math.max(maxZ, corner.getZ());
                }
            }
        }
        return new AABB(minX - 1.0, minY - 1.0, minZ - 1.0, maxX + 2.0, maxY + 2.0, maxZ + 2.0);
    }

    // ---------------------------------------------------------------- 工具

    @Unique
    private static boolean within(BlockPos min, BlockPos max, BlockPos p) {
        return p.getX() >= min.getX() && p.getX() <= max.getX()
                && p.getY() >= min.getY() && p.getY() <= max.getY()
                && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
    }
}
