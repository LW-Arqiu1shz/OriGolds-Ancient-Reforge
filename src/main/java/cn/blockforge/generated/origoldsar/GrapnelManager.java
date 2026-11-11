package cn.blockforge.generated.origoldsar;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 钩爪弩炮的绑定与拖拽核心。
 *
 * <p>绑定的是“主动射出的那根主箭”，记录成箭矢实体的 UUID 以便每 tick 动态追踪，
 * 因此箭插在方块上、还在飞、插在生物身上都能实时取出它当前的位置。
 *
 * <p>四种用法：
 * <ul>
 *   <li>命中方块 / 仍在飞 / 未命中且已消失之后——左键把玩家 32 格/秒拉向目标（被方块挡住）；</li>
 *   <li>命中生物 + 潜行 + 左键——把上一次射击命中的所有生物 16 格/秒拉向玩家（穿墙）；</li>
 *   <li>命中生物 + 左键——把玩家 32 格/秒拉向第一个命中的生物（穿墙、动态追踪）；</li>
 *   <li>潜行 + 中键（或下一次射击）——清除绑定并中断所有拖拽。</li>
 * </ul>
 */
public final class GrapnelManager {
    /** 32 格/秒 = 1.6 格/tick。 */
    public static final double PULL_PLAYER_SPEED = 32.0D / 20.0D;
    /** 16 格/秒 = 0.8 格/tick。 */
    public static final double PULL_MOB_SPEED = 16.0D / 20.0D;
    /**
     * 额外排除标签：实体类型打上该标签即免疫钩爪（作为带血条 boss 检测之外的补充，
     * 供数据包/作者显式指定）。默认列表为空——劫掠兽等无血条生物不再被静态名单误禁。
     */
    public static final TagKey<EntityType<?>> UNPULLABLE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "grapnel_unpullable"));

    private static final int PULL_NONE = 0;
    private static final int PULL_PLAYER_TO_POINT = 1;
    private static final int PULL_PLAYER_TO_ENTITY = 2;
    private static final int PULL_MOBS_TO_PLAYER = 3;

    private static final Map<UUID, BindState> BINDS = new HashMap<>();
    private static final Map<UUID, PullState> PULLS = new HashMap<>();
    /** 玩家是否开启了“路径预览”（Ctrl + 中键切换），用于决定服务端是否流式同步钩爪目标。 */
    private static final Map<UUID, Boolean> PREVIEW = new ConcurrentHashMap<>();

    private GrapnelManager() {}

    /** 单次绑定的全部状态：主箭、它命中过什么、以及按下左键那一刻的目标快照。 */
    private static final class BindState {
        UUID arrow;
        /** 从未命中任何东西就消失了（被 /kill、自己消失），此时视为“没有可使用的钩爪”。 */
        boolean vanished;
        boolean hitBlock;
        /** 按命中先后排序的去重活体命中列表（穿透箭会命中多个）。 */
        final LinkedHashSet<UUID> hitMobs = new LinkedHashSet<>();
        final Map<UUID, Vec3> lastMobPos = new HashMap<>();
        Vec3 lastPos;
    }

    /** 一次正在进行的拖拽；targetPos/lastMobPos 每 tick 刷新以实现动态追踪。 */
    private static final class PullState {
        int type;
        Vec3 targetPos;
        UUID targetEntity;
        final Map<UUID, Vec3> mobTargets = new HashMap<>();
    }

    // ------------------------------------------------------------------
    // 绑定
    // ------------------------------------------------------------------

    public static void bind(UUID playerId, UUID arrowId, Vec3 pos) {
        BindState state = new BindState();
        state.arrow = arrowId;
        state.lastPos = pos;
        BINDS.put(playerId, state);
    }

    public static boolean hasBinding(UUID playerId) {
        return BINDS.containsKey(playerId);
    }

    /** 射出新箭时自动清除上一次的绑定（同时中断拖拽）。 */
    public static void clear(UUID playerId) {
        BINDS.remove(playerId);
        PULLS.remove(playerId);
    }

    public static void recordBlockHit(UUID playerId, UUID arrowId) {
        BindState state = BINDS.get(playerId);
        if (state != null && arrowId.equals(state.arrow)) {
            state.hitBlock = true;
        }
    }

    public static void recordEntityHit(UUID playerId, UUID arrowId, LivingEntity target) {
        BindState state = BINDS.get(playerId);
        if (state == null || !arrowId.equals(state.arrow)) {
            return;
        }
        state.hitMobs.add(target.getUUID());
        state.lastMobPos.put(target.getUUID(), target.position());
    }

    private static ServerLevel serverLevel(Player player) {
        return player.level() instanceof ServerLevel serverLevel ? serverLevel : null;
    }

    /**
     * 每 tick 在主箭仍然存在时刷新“最后已知坐标”，让飞行中的箭、插在方块上的箭、
     * 插在生物身上被带走的箭都能被动态追踪。
     */
    public static void tickArrowTracking(Player player) {
        BindState state = BINDS.get(player.getUUID());
        if (state == null || state.vanished) {
            return;
        }
        ServerLevel level = serverLevel(player);
        if (level == null) {
            return;
        }
        Entity arrow = level.getEntity(state.arrow);
        if (arrow == null) {
            // 箭已经从世界移除：命中生物后弩箭会被消耗/嵌入目标（无穿透时直接消失），
            // 或被 /kill、穿过边界等。只有“从未命中过任何东西”才说明没有可使用的钩爪；
            // 只要命中过生物或方块，靠 hitMobs（按命中先后排序）或最后已知坐标仍能选定目标，
            // 绑定必须保持有效，否则左键拉人/潜行拉怪都会报“没有可使用的钩爪”。
            if (!state.hitBlock && state.hitMobs.isEmpty()) {
                state.vanished = true;
            }
        } else {
            state.lastPos = arrow.position();
        }
    }

    /** 下一次发射/潜行中键时用到的统一清除入口。 */
    public static void breakBinding(Player player) {
        clear(player.getUUID());
    }

    /**
     * 潜行 + 右键：立刻中断当前拖拽（正在被拉过去时“下车”），
     * 但保留与箭矢的绑定——之后左键还能继续钩。
     */
    public static void dropPull(Player player) {
        PULLS.remove(player.getUUID());
        // 下车即停：清掉本 tick 已被拖动赋予的速度，避免惯性继续滑行。
        player.setDeltaMovement(0.0D, player.getDeltaMovement().y, 0.0D);
        player.fallDistance = 0.0F;
    }

    // ------------------------------------------------------------------
    // 左键：拉人 / 拉生物
    // ------------------------------------------------------------------

    /** 左键单击。潜行且命中过生物时拉生物，否则按“命中生物优先，其次箭矢坐标”拉玩家。 */
    public static void onLeftClick(ServerPlayer player, boolean sneaking) {
        BindState state = BINDS.get(player.getUUID());
        if (state == null || state.vanished) {
            message(player, "message.origolds_ar.grapnel_no_hook");
            return;
        }
        if (sneaking) {
            List<Entity> targets = collectPullableMobs(player, state);
            if (targets.isEmpty()) {
                message(player, "message.origolds_ar.grapnel_no_mob");
                return;
            }
            startPullMobs(player, targets);
            return;
        }
        startPullPlayer(player, state);
    }

    private static List<Entity> collectPullableMobs(ServerPlayer player, BindState state) {
        List<Entity> targets = new ArrayList<>();
        ServerLevel level = serverLevel(player);
        if (level == null) {
            return targets;
        }
        for (UUID id : state.hitMobs) {
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive() && !isUnpullable(entity)) {
                targets.add(entity);
            }
        }
        return targets;
    }

    private static boolean isUnpullable(Entity entity) {
        return entity.getType().is(UNPULLABLE) || hasBossHealthBar(entity);
    }

    // ------------------------------------------------------------------
    // “带血条 boss”检测
    // ------------------------------------------------------------------
    // Minecraft 1.21.1 的 Boss 血条并不是服务端可枚举的注册表，而是 boss 实体自己持有一个
    // ServerBossEvent 字段（末影龙是特例，其血条挂在 EndDragonFight 上）。因此最通用的判定
    // 是：反射扫描实体类层级中是否有非空、可见的 ServerBossEvent 字段——这能覆盖原版凋灵、
    // 以及任意附属模组（如 origolds_ga）里所有“带血条”的 boss，而劫掠兽这类无血条生物不在其列。
    private static final Class<?> BOSS_EVENT_TYPE = net.minecraft.server.level.ServerBossEvent.class;
    /** 缓存每个实体类“是否有带血条的性质”，避免每 tick 反射。 */
    private static final Map<Class<?>, Boolean> BOSS_FLAG_CACHE = new ConcurrentHashMap<>();

    private static boolean hasBossHealthBar(Entity entity) {
        // 末影龙在实体本身并不持有 ServerBossEvent（血条在 EndDragonFight），单独特判。
        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            return true;
        }
        Boolean cached = BOSS_FLAG_CACHE.get(entity.getClass());
        if (cached == null) {
            cached = scansBossEventField(entity.getClass());
            BOSS_FLAG_CACHE.put(entity.getClass(), cached);
        }
        // 缓存的是“类里有没有血条字段”；是否真的在显示，再看该字段当前是否非空且可见。
        return cached && bossEventShown(entity);
    }

    /** 类层级里是否存在类型为 {@link net.minecraft.server.level.ServerBossEvent} 的字段。 */
    private static boolean scansBossEventField(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (BOSS_EVENT_TYPE.isAssignableFrom(field.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 该 boss 实体的血条字段当前是否非空且处于可见状态。 */
    private static boolean bossEventShown(Entity entity) {
        for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!BOSS_EVENT_TYPE.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (value instanceof net.minecraft.server.level.ServerBossEvent event && event.isVisible()) {
                        return true;
                    }
                } catch (IllegalAccessException | RuntimeException ignored) {
                    // 拿不到/不兼容就跳过该字段，继续找下一个。
                }
            }
        }
        return false;
    }

    /** 情况 A / B2：把玩家拉向目标点（命中生物时优先追踪第一个命中的生物）。 */
    private static void startPullPlayer(ServerPlayer player, BindState state) {
        ServerLevel level = serverLevel(player);
        if (level == null) {
            return;
        }
        PullState pull = new PullState();
        LivingEntity first = firstLivingHit(level, state);
        if (first != null) {
            pull.type = PULL_PLAYER_TO_ENTITY;
            pull.targetEntity = first.getUUID();
            pull.targetPos = first.position();
        } else {
            pull.type = PULL_PLAYER_TO_POINT;
            pull.targetPos = state.lastPos != null ? state.lastPos : player.position();
        }
        PULLS.put(player.getUUID(), pull);
    }

    private static LivingEntity firstLivingHit(ServerLevel level, BindState state) {
        for (UUID id : state.hitMobs) {
            Entity entity = level.getEntity(id);
            // 不可被“抓取”的实体（带血条 boss、被打上排除标签的目标）仍然可以直接左键钩上去，
            // 即把玩家拉向该实体本身；只是不能反过来把它拽向玩家（见 collectPullableMobs）。
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    /** 情况 B1：把所有命中的生物拉向玩家（无视方块阻挡、动态追踪玩家）。 */
    private static void startPullMobs(ServerPlayer player, List<Entity> targets) {
        PullState pull = new PullState();
        pull.type = PULL_MOBS_TO_PLAYER;
        for (Entity entity : targets) {
            pull.mobTargets.put(entity.getUUID(), entity.position());
        }
        PULLS.put(player.getUUID(), pull);
    }

    // ------------------------------------------------------------------
    // 每 tick 推进拖拽
    // ------------------------------------------------------------------

    public static void tickPull(ServerPlayer player) {
        PullState pull = PULLS.get(player.getUUID());
        if (pull == null) {
            return;
        }
        ServerLevel level = serverLevel(player);
        if (level == null) {
            PULLS.remove(player.getUUID());
            return;
        }
        if (pull.type == PULL_MOBS_TO_PLAYER) {
            tickPullMobs(player, level, pull);
        } else {
            tickPullPlayer(player, level, pull);
        }
    }

    private static void tickPullPlayer(ServerPlayer player, ServerLevel level, PullState pull) {
        Vec3 target = pull.targetPos;
        if (pull.type == PULL_PLAYER_TO_ENTITY) {
            Entity entity = level.getEntity(pull.targetEntity);
            if (entity != null) {
                // 目标生物还活着就实时追踪；死了就停在它最后的坐标。
                target = entity.position();
                pull.targetPos = target;
            }
        }
        if (target == null) {
            PULLS.remove(player.getUUID());
            return;
        }

        AABB playerBox = player.getBoundingBox();
        AABB targetBox = new AABB(
                target.x - 0.3D, target.y - 0.3D, target.z - 0.3D,
                target.x + 0.3D, target.y + 0.3D, target.z + 0.3D);
        if (playerBox.intersects(targetBox)) {
            // 碰撞箱接触：传送到目标位置并解除钩爪控制。
            player.teleportTo(target.x, target.y, target.z);
            PULLS.remove(player.getUUID());
            return;
        }

        Vec3 direction = target.subtract(player.position());
        double distance = direction.length();
        if (distance < 1.0E-4D) {
            PULLS.remove(player.getUUID());
            return;
        }
        double step = Math.min(PULL_PLAYER_SPEED, distance);
        Vec3 motion = direction.normalize().scale(step);
        // 被方块挡住：逐步缩短本 tick 的位移，停在方块前并结束拖拽。
        Vec3 allowed = clipByBlock(level, player, motion);
        if (allowed == null) {
            PULLS.remove(player.getUUID());
            return;
        }
        player.setDeltaMovement(allowed);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
    }

    private static void tickPullMobs(ServerPlayer player, ServerLevel level, PullState pull) {
        for (UUID id : new ArrayList<>(pull.mobTargets.keySet())) {
            Entity entity = level.getEntity(id);
            if (entity == null || !entity.isAlive() || isUnpullable(entity)) {
                pull.mobTargets.remove(id);
                continue;
            }
            Vec3 direction = player.position().subtract(entity.position());
            double distance = direction.length();
            if (distance < 1.0E-4D) {
                pull.mobTargets.remove(id);
                continue;
            }
            double step = Math.min(PULL_MOB_SPEED, distance);
            Vec3 next = entity.position().add(direction.normalize().scale(step));
            // 穿墙：直接写坐标，不做碰撞检测。
            entity.setPos(next.x, next.y, next.z);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hurtMarked = true;
            pull.mobTargets.put(id, entity.position());

            if (entity.getBoundingBox().intersects(player.getBoundingBox())) {
                entity.teleportTo(player.getX(), player.getY(), player.getZ());
                pull.mobTargets.remove(id);
            }
        }
        if (pull.mobTargets.isEmpty()) {
            PULLS.remove(player.getUUID());
        }
    }

    /**
     * 本 tick 位移能否通过：整段被挡时不断对半缩短，直到找出不穿墙的最大位移。
     * 返回 {@code null} 表示整体被挡住（一步都走不了），需要结束拖拽。
     */
    private static Vec3 clipByBlock(ServerLevel level, Player player, Vec3 motion) {
        Vec3 step = motion;
        for (int i = 0; i < 8; i++) {
            Vec3 from = player.position();
            Vec3 to = from.add(step);
            HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                return step;
            }
            step = step.scale(0.5D);
        }
        return null;
    }

    private static void message(ServerPlayer player, String key) {
        PlayerInfoBar.show(player, net.minecraft.network.chat.Component.translatable(key));
    }

    /** 玩家下线/切换维度时让绑定与拖拽自然结束。 */
    public static void forget(UUID playerId) {
        BINDS.remove(playerId);
        PULLS.remove(playerId);
        PREVIEW.remove(playerId);
    }

    // ------------------------------------------------------------------
    // 路径预览
    // ------------------------------------------------------------------

    /** 切换/记录某玩家是否开启了钩爪路径预览（服务端据此决定是否流式同步目标位置）。 */
    public static void setPreviewEnabled(UUID playerId, boolean enabled) {
        PREVIEW.put(playerId, enabled);
    }

    /** 该玩家当前是否开启了路径预览。 */
    public static boolean isPreviewEnabled(UUID playerId) {
        return PREVIEW.getOrDefault(playerId, false);
    }

    /**
     * 计算该玩家当前钩爪会飞向的目标坐标，供路径预览展示。
     * 命中生物时追踪第一个命中的目标，否则回落到箭矢最后已知坐标；
     * 没有有效绑定时返回 {@code null}。
     */
    public static Vec3 previewTarget(ServerPlayer player) {
        BindState state = BINDS.get(player.getUUID());
        if (state == null || state.vanished) {
            return null;
        }
        ServerLevel level = serverLevel(player);
        if (level == null) {
            return null;
        }
        LivingEntity first = firstLivingHit(level, state);
        if (first != null) {
            return first.position();
        }
        return state.lastPos;
    }

    /** 给外部（如清理时）查询当前是否正在被拖拽，便于调试与命令扩展。 */
    public static boolean isPulling(UUID playerId) {
        return PULLS.containsKey(playerId);
    }

    /** 当前绑定箭矢的最后已知坐标，没有绑定时返回 {@code null}。 */
    public static Vec3 boundPosition(UUID playerId) {
        BindState state = BINDS.get(playerId);
        return state == null ? null : state.lastPos;
    }

    /** 当前绑定是否已经被判定为“消失且从未命中”。 */
    public static boolean bindingVanished(UUID playerId) {
        BindState state = BINDS.get(playerId);
        return state == null || state.vanished;
    }

    /** 让类型标签判定也能从外部（命令、其它事件）复用。 */
    public static boolean isUnpullableType(EntityType<?> type) {
        return type.is(UNPULLABLE);
    }
}
