package cn.blockforge.generated.origoldsar.purse;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 异界行囊的世界规则：
 * <ul>
 *   <li>死亡时物品栏里的行囊不掉落，重生后还给玩家（装备着的行囊本来就随附件保留）；</li>
 *   <li>扔出来的行囊不受重力（顺着视线飞，阻力大，最多飞 4 格左右）、不会消失、不会被岩浆仙人掌等伤到；</li>
 *   <li>被 /kill 之类强行清除时，把里面的物品撒出来。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class PurseEvents {
    /** 扔出去时的速度（格/刻）。 */
    private static final double THROW_SPEED = 0.35;
    /** 每刻额外乘上的阻力系数。 */
    private static final double DRAG = 0.92;

    private PurseEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return;
        }
        Inventory inventory = player.getInventory();
        List<ItemStack> kept = player.getData(GeneratedMod.PURSE_DATA).kept();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (OtherworldPurseItem.isPurse(stack)) {
                kept.add(stack);
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        // 鼠标上拿着的行囊（死的时候界面还开着）也收走。
        ItemStack carried = player.containerMenu.getCarried();
        if (OtherworldPurseItem.isPurse(carried)) {
            kept.add(carried);
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        List<ItemStack> kept = player.getData(GeneratedMod.PURSE_DATA).kept();
        if (!kept.isEmpty()) {
            for (ItemStack stack : new ArrayList<>(kept)) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false, net.minecraft.util.Prediction.PREDICTED);
                }
            }
            kept.clear();
        }
        PurseNetwork.sync(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PurseNetwork.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PurseNetwork.sync(player);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item)
                || !OtherworldPurseItem.isPurse(item.getItem())) {
            return;
        }
        item.setNoGravity(true);
        item.setPermanentlyInvulnerable(true);
        item.setUnlimitedLifetime();
        if (event.loadedFromDisk()) {
            return;
        }
        if (item.getOwner() instanceof net.minecraft.world.entity.player.Player thrower) {
            // 玩家扔出来的：顺着视线方向直直飞出去（没有重力，所以不要原版那点往上抛的速度）。
            item.setDeltaMovement(thrower.getLookAngle().scale(THROW_SPEED));
        } else {
            // 其它方式掉出来的：没有重力时向上的初速度会让它一直往上飘。
            Vec3 motion = item.getDeltaMovement();
            item.setDeltaMovement(motion.x, 0.0, motion.z);
        }
    }

    /**
     * 额外的空气阻力：每刻再乘一次 {@link #DRAG}。连同原版的 0.98，
     * 以 {@link #THROW_SPEED} 扔出时总共飞 0.35 / (1 - 0.98 × 0.92) ≈ 3.6 格就停下，不会到处乱飘。
     */
    @SubscribeEvent
    public static void onEntityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ItemEntity item && OtherworldPurseItem.isPurse(item.getItem())) {
            Vec3 motion = item.getDeltaMovement();
            if (motion.lengthSqr() > 1.0E-8) {
                item.setDeltaMovement(motion.scale(DRAG));
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ItemEntity item)) {
            return;
        }
        ItemStack stack = item.getItem();
        Entity.RemovalReason reason = item.getRemovalReason();
        if (!OtherworldPurseItem.isPurse(stack) || reason == null || !reason.shouldDestroy()) {
            return;
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return;
        }
        List<ItemStack> spilled = new ArrayList<>();
        contents.nonEmptyItemCopyStream().forEach(spilled::add);
        stack.remove(DataComponents.CONTAINER);
        if (spilled.isEmpty()) {
            return;
        }
        double x = item.getX();
        double y = item.getY();
        double z = item.getZ();
        MinecraftServer server = level.getServer();
        server.schedule(new TickTask(server.getTickCount(), () -> {
            for (ItemStack drop : spilled) {
                ItemEntity entity = new ItemEntity(level, x, y, z, drop);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
            }
        }));
    }
}
