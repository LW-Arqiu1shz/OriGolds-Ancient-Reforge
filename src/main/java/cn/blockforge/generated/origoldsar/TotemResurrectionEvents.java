package cn.blockforge.generated.origoldsar;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 复苏图腾的触发逻辑：玩家死亡前，若背包中（任意一格）存在一根“未在冷却中”的复苏图腾，
 * 则取消死亡并施加与原版不死图腾一致的复活效果；随后让该玩家身上的所有复苏图腾
 * （无论是哪一格）一起进入 180 秒冷却。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class TotemResurrectionEvents {
    /** 四个护甲槽（26.1.2 移除了 Inventory#armor 与 Player#getArmorSlots）。 */
    private static final net.minecraft.world.entity.EquipmentSlot[] ARMOR_SLOTS = {
            net.minecraft.world.entity.EquipmentSlot.HEAD,
            net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.LEGS,
            net.minecraft.world.entity.EquipmentSlot.FEET};

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
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (isReadyTotem(stack)) {
                return true;
            }
        }
        for (net.minecraft.world.entity.EquipmentSlot armorSlot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(armorSlot);
            if (isReadyTotem(stack)) {
                return true;
            }
        }
        if (isReadyTotem(player.getOffhandItem())) {
            return true;
        }
        return false;
    }

    /** 让该玩家身上（主背包/护甲/副手）的所有复苏图腾一起进入冷却。 */
    private static void putAllTotemsOnCooldown(Player player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (TotemOfResurrectionItem.isTotem(stack)) {
                TotemOfResurrectionItem.startCooldown(stack);
            }
        }
        for (net.minecraft.world.entity.EquipmentSlot armorSlot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(armorSlot);
            if (TotemOfResurrectionItem.isTotem(stack)) {
                TotemOfResurrectionItem.startCooldown(stack);
            }
        }
        if (TotemOfResurrectionItem.isTotem(player.getOffhandItem())) {
            TotemOfResurrectionItem.startCooldown(player.getOffhandItem());
        }
    }

    private static boolean isReadyTotem(ItemStack stack) {
        return TotemOfResurrectionItem.isTotem(stack)
                && !TotemOfResurrectionItem.isOnCooldown(stack);
    }

    private static void revive(Player player) {
        player.setHealth(1.0F);
        // 26.x 起没有 EffectCures，图腾的“清除负面效果”直接按效果分类做。
        for (var effect : java.util.List.copyOf(player.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        player.level().broadcastEntityEvent(player, (byte) 35);
    }
}
