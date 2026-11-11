package cn.blockforge.generated.origoldsar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Set;

/**
 * 为原版回溯指针（{@link Items#RECOVERY_COMPASS}，即追溯指针）注入新功能：
 * 右键使用后把持有者传送回自己的重生点；若从未设置过重生点、或重生点所在维度不可用 /
 * 落点被方块阻挡而无处落脚，则退回主世界的默认出生点。
 *
 * <p>每次成功使用（右键）后，物品进入 10 秒<strong>全局冷却</strong>——和原版末影珍珠一样，
 * 通过 {@link net.minecraft.world.item.ItemCooldowns#addCooldown} 按物品类型记录冷却，
 * 物品栏与快捷栏里所有该物品会自动显示冷却条，无需任何自定义持久化组件。
 * 全程通过 NeoForge 事件实现，不使用 Mixin。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class RecoveryCompassEvents {
    private RecoveryCompassEvents() {}

    /** 使用追溯指针后的全局冷却时长：10 秒（20 tick/秒）。 */
    private static final int COOLDOWN_TICKS = 200;

    /** 以重生点为圆心搜索安全落点的最大水平半径（格）；找不到即视为“被阻挡”。 */
    private static final int RESPAWN_SEARCH_RADIUS = 3;
    /** 主世界出生点周边的兜底搜索半径（格）。 */
    private static final int WORLD_SPAWN_SEARCH_RADIUS = 16;
    /** 单列内上下微调落点的高度容差（格）。 */
    private static final int VERTICAL_TOLERANCE = 3;

    @SubscribeEvent
    public static void onRightClickCompass(PlayerInteractEvent.RightClickItem event) {
        // 追溯指针（原版回溯指针）本身没有任何右键行为，这里在服务端注入“传送回重生点”。
        if (event.getLevel().isClientSide() || !event.getItemStack().is(Items.RECOVERY_COMPASS)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        event.setCanceled(true);
        // 全局冷却：26.1.2 起冷却按物品堆栈记录，这里用当前这一堆。
        ItemStack compass = event.getItemStack();
        if (player.getCooldowns().isOnCooldown(compass)) {
            event.setCancellationResult(InteractionResult.PASS);
            return;
        }
        // 原版全局冷却（同末影珍珠机制），对该物品堆栈通用，并自动渲染冷却条。
        player.getCooldowns().addCooldown(compass, COOLDOWN_TICKS);
        if (teleportToRespawn(player)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
        } else {
            PlayerInfoBar.show(player, Component.translatable("origolds_ar.message.recovery_compass_blocked"));
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    /**
     * 执行传送：优先玩家重生点，其次主世界默认出生点。
     *
     * @return 成功传送返回 true；两个目的地都找不到安全落点返回 false。
     */
    private static boolean teleportToRespawn(ServerPlayer player) {
        if (player.isPassenger()) {
            player.stopRiding();
        }
        MinecraftServer server = player.level().getServer();

        // 1) 玩家自己的重生点：设置过、维度存在且附近能落脚才算可用。
        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        GlobalPos respawn = respawnConfig == null ? null : respawnConfig.respawnData().globalPos();
        if (respawn != null) {
            BlockPos respawnPos = respawn.pos();
            ResourceKey<Level> dimKey = respawn.dimension();
            ServerLevel respawnLevel = server.getLevel(dimKey);
            if (respawnLevel != null) {
                BlockPos safe = findSafeSpot(respawnLevel, respawnPos, RESPAWN_SEARCH_RADIUS);
                if (safe != null) {
                    teleport(player, respawnLevel, safe, player.getYRot(), player.getXRot());
                    return true;
                }
            }
            // 重生点被阻挡（床被拆、落点埋进方块等）：退回主世界出生点。
        }

        // 2) 主世界默认出生点（世界初始出生点与 /setspawn 都在这里）。
        ServerLevel overworld = server.overworld();
        BlockPos worldSpawn = overworld.getRespawnData().pos();
        BlockPos safe = findSafeSpot(overworld, worldSpawn, WORLD_SPAWN_SEARCH_RADIUS);
        if (safe == null) {
            return false;
        }
        teleport(player, overworld, safe, overworld.getRespawnData().yaw(), 0.0F);
        return true;
    }

    /** 传送并在出发点、落点各播放一次末影人式的传送音效与 end_rod 粒子。 */
    private static void teleport(ServerPlayer player, ServerLevel target, BlockPos spot,
                                 float yaw, float pitch) {
        ServerLevel current = player.level();
        Vec3 origin = player.position();
        current.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        current.sendParticles(ParticleTypes.END_ROD,
                origin.x, origin.y + 1.0D, origin.z, 40, 0.5D, 1.0D, 0.5D, 0.01D);
        player.gameEvent(GameEvent.TELEPORT);

        double x = spot.getX() + 0.5D;
        double y = spot.getY();
        double z = spot.getZ() + 0.5D;
        player.teleportTo(target, x, y, z, Set.<Relative>of(), yaw, pitch, true);
        player.setDeltaMovement(Vec3.ZERO);

        target.playSound(null, x, y, z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        target.sendParticles(ParticleTypes.END_ROD,
                x, y + 1.0D, z, 40, 0.5D, 1.0D, 0.5D, 0.01D);
        player.gameEvent(GameEvent.TELEPORT);
    }

    /**
     * 在 {@code center} 附近由近及远搜索一个能安全站人的落点：
     * 先试中心格，再逐环向外、每列上下微调；超出半径仍找不到则返回 null，
     * 即视为该区域被阻挡。
     */
    private static BlockPos findSafeSpot(ServerLevel level, BlockPos center, int maxRadius) {
        for (int r = 0; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // 只扫描当前半径的方环，保证由近及远。
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int dy = 0; dy <= VERTICAL_TOLERANCE; dy++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (isSafeSpot(level, pos)) {
                            return pos;
                        }
                        if (dy > 0) {
                            pos = center.offset(dx, -dy, dz);
                            if (isSafeSpot(level, pos)) {
                                return pos;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /** 判断某格能否安全站立：脚下有支撑、头脚两格无碰撞、周围无熔岩与火。 */
    private static boolean isSafeSpot(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos)) {
            return false;
        }
        ChunkPos chunk = ChunkPos.containing(pos);
        if (level.getChunkSource().getChunk(chunk.x(), chunk.z(), ChunkStatus.FULL, true) == null) {
            return false; // 区块实在加载不出来，跳过该候选点。
        }
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        // 26.3：BlockState#blocksMotion 已删除，等价判定改为看碰撞形状是否为空。
        if (!feet.getCollisionShape(level, pos).isEmpty()
                || !head.getCollisionShape(level, pos.above()).isEmpty()) {
            return false; // 身体会被方块卡住。
        }
        if (below.getCollisionShape(level, pos.below()).isEmpty()) {
            return false; // 脚下无支撑，会掉下去。
        }
        for (BlockState state : new BlockState[]{below, feet, head}) {
            if (state.getFluidState().is(FluidTags.LAVA)
                    || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                return false;
            }
        }
        return true;
    }
}
