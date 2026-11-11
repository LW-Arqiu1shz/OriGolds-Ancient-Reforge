package cn.blockforge.generated.origoldsar;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 钩爪弩炮的服务端接线：
 * <ul>
 *   <li>射出的主箭加入世界时建立绑定（同时清掉旧绑定）；</li>
 *   <li>箭命中方块/生物时记录命中信息，供后续拉拽选目标；</li>
 *   <li>每 tick 刷新绑定箭矢坐标并推进正在进行的拖拽；</li>
 *   <li>玩家下线时清掉绑定与拖拽状态。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class GrapnelEvents {
    private GrapnelEvents() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Projectile projectile)) {
            return;
        }
        if (!(projectile.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        if (!weaponOf(projectile).is(GeneratedMod.GRAPNEL_BALLISTA.get())) {
            return;
        }
        // 主箭是同一轮射击中最后被打上标记的那根：多重射击的附加箭是在主箭之后创建的，
        // 会把标记覆盖掉，于是最终只剩主箭带标记。
        if (projectile instanceof AbstractArrow arrow && GrapnelBallista.isMarkedMainArrow(arrow)) {
            GrapnelManager.bind(player.getUUID(), arrow.getUUID(), arrow.position());
        }
        GrapnelBallista.clearMainArrowMark(projectile);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        if (!weaponOf(arrow).is(GeneratedMod.GRAPNEL_BALLISTA.get())) {
            return;
        }
        HitResult hit = event.getRayTraceResult();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            GrapnelManager.recordEntityHit(player.getUUID(), arrow.getUUID(), living);
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            GrapnelManager.recordBlockHit(player.getUUID(), arrow.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        GrapnelManager.tickArrowTracking(player);
        GrapnelManager.tickPull(player);
        syncPreviewTarget(player);
    }

    /** 手持弩且开启了路径预览的玩家，每 tick 把当前钩爪目标位置同步给客户端。 */
    private static void syncPreviewTarget(ServerPlayer player) {
        if (!holdsBallista(player) || !GrapnelManager.isPreviewEnabled(player.getUUID())) {
            return;
        }
        Vec3 target = GrapnelManager.previewTarget(player);
        // 只在目标变化时发包（插在方块上的箭每 tick 都是同一个坐标），另每秒兜底重发一次。
        Vec3 last = LAST_PREVIEW.get(player.getUUID());
        boolean changed = target == null ? last != NO_TARGET : !target.equals(last);
        if (!changed && player.tickCount % 20 != 0) {
            return;
        }
        LAST_PREVIEW.put(player.getUUID(), target == null ? NO_TARGET : target);
        if (target == null) {
            PacketDistributor.sendToPlayer(player, new GrapnelPreviewPayload(false, 0.0, 0.0, 0.0));
        } else {
            PacketDistributor.sendToPlayer(player, new GrapnelPreviewPayload(true, target.x, target.y, target.z));
        }
    }

    /** 每个玩家上一次发出去的预览目标（{@link #NO_TARGET} 表示“无目标”）。 */
    private static final java.util.Map<java.util.UUID, Vec3> LAST_PREVIEW = new java.util.HashMap<>();
    private static final Vec3 NO_TARGET = new Vec3(Double.NaN, Double.NaN, Double.NaN);

    /**
     * 换维度 / 重生时结束钩爪：绑定的箭和记下的坐标都属于原来的维度（或死前的位置），
     * 留着的话正在进行的拖拽会在新维度里按旧坐标继续拽人，可能一路拖过大片未加载地形。
     */
    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        GrapnelManager.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        GrapnelManager.clear(event.getEntity().getUUID());
    }

    private static boolean holdsBallista(ServerPlayer player) {
        return player.getMainHandItem().is(GeneratedMod.GRAPNEL_BALLISTA.get())
                || player.getOffhandItem().is(GeneratedMod.GRAPNEL_BALLISTA.get());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GrapnelManager.forget(event.getEntity().getUUID());
        LAST_PREVIEW.remove(event.getEntity().getUUID());
    }

    private static ItemStack weaponOf(Projectile projectile) {
        if (projectile instanceof AbstractArrow arrow) {
            ItemStack stack = arrow.getWeaponItem();
            return stack == null ? ItemStack.EMPTY : stack;
        }
        return ItemStack.EMPTY;
    }
}
