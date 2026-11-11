package cn.blockforge.generated.origoldsar;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 复苏图腾的触发逻辑：玩家死亡前，若背包中（任意一格）存在一根“未在冷却中”的复苏图腾，
 * 则取消死亡并施加与原版不死图腾一致的复活效果；随后让该玩家身上的所有复苏图腾
 * （无论是哪一格）一起进入 180 秒冷却。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class TotemResurrectionEvents {
    private TotemResurrectionEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        // 与原版不死图腾一致：无视无敌/虚空等绕过无敌的伤害，不触发复活。
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (!hasReadyTotemInInventory(player)) {
            return;
        }
        // 取消死亡，施加复活效果，并让该玩家身上的所有复苏图腾一起进入冷却。
        event.setCanceled(true);
        revive(player);
        putAllTotemsOnCooldown(player);
    }

    /** 该玩家身上（主背包/护甲/副手）是否存在一根未在冷却中的复苏图腾。 */
    private static boolean hasReadyTotemInInventory(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isReadyTotem(stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (isReadyTotem(stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (isReadyTotem(stack)) {
                return true;
            }
        }
        return false;
    }

    /** 让该玩家身上（主背包/护甲/副手）的所有复苏图腾一起进入冷却。 */
    private static void putAllTotemsOnCooldown(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (TotemOfResurrectionItem.isTotem(stack)) {
                TotemOfResurrectionItem.startCooldown(stack);
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (TotemOfResurrectionItem.isTotem(stack)) {
                TotemOfResurrectionItem.startCooldown(stack);
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (TotemOfResurrectionItem.isTotem(stack)) {
                TotemOfResurrectionItem.startCooldown(stack);
            }
        }
    }

    private static boolean isReadyTotem(ItemStack stack) {
        return TotemOfResurrectionItem.isTotem(stack)
                && !TotemOfResurrectionItem.isOnCooldown(stack);
    }

    private static void revive(Player player) {
        player.setHealth(1.0F);
        player.removeEffectsCuredBy(EffectCures.PROTECTED_BY_TOTEM);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        player.level().broadcastEntityEvent(player, (byte) 35);
    }
}
