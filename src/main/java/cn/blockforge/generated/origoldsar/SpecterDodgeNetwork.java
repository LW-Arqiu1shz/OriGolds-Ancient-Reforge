package cn.blockforge.generated.origoldsar;

import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SpecterDodgeNetwork {
    private static final net.minecraft.world.entity.EquipmentSlot[] ARMOR_SLOTS = {
            net.minecraft.world.entity.EquipmentSlot.HEAD,
            net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.LEGS,
            net.minecraft.world.entity.EquipmentSlot.FEET};

    public static final int INVULNERABILITY_TICKS = 15;
    public static final int COOLDOWN_TICKS = 45;
    private static final String COOLDOWN_TAG = "origolds_ar_dodge_cooldown";
    private static final String INVULNERABILITY_TAG = "origolds_ar_dodge_invulnerable_until";

    private SpecterDodgeNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                SpecterDodgePayload.TYPE,
                SpecterDodgePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var player = context.player();
                    if (player != null && hasMedallion(player)
                            && player.getPersistentData().getLongOr(COOLDOWN_TAG, 0L) <= player.level().getGameTime()) {
                        long now = player.level().getGameTime();
                        player.getPersistentData().putLong(COOLDOWN_TAG, now + COOLDOWN_TICKS);
                        player.getPersistentData().putLong(INVULNERABILITY_TAG, now + INVULNERABILITY_TICKS);
                        player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 3.0F, 1.0F);
                    }
                }));
    }

    public static boolean isDodgeInvulnerable(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getPersistentData().getLongOr(INVULNERABILITY_TAG, 0L) > entity.level().getGameTime();
    }

    private static boolean hasMedallion(net.minecraft.world.entity.player.Player player) {
        for (var stack : player.getInventory().getNonEquipmentItems()) if (AbilityItems.hasMedallionAbility(stack, GeneratedMod.SPECTER_MEDALLION.get())) return true;
        for (var armorSlot : ARMOR_SLOTS) if (AbilityItems.hasMedallionAbility(player.getItemBySlot(armorSlot), GeneratedMod.SPECTER_MEDALLION.get())) return true;
        return AbilityItems.hasMedallionAbility(player.getOffhandItem(), GeneratedMod.SPECTER_MEDALLION.get());
    }
}
