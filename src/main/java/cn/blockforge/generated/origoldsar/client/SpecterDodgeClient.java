package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.AbilityItems;
import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.SpecterDodgePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class SpecterDodgeClient {
    private static final int DOUBLE_PRESS_TICKS = 8;
    private static final int COOLDOWN_TICKS = 45;
    private static final KeyMapping DODGE = OrigoldKeyBindings.DODGE;
    private static int lastPressTick = -1000;
    private static int clientTick;
    private static int cooldownUntil;

    private SpecterDodgeClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        clientTick++;
        while (DODGE.consumeClick()) {
            if (clientTick - lastPressTick <= DOUBLE_PRESS_TICKS) activate(player);
            lastPressTick = clientTick;
        }
    }

    private static void activate(Player player) {
        if (clientTick < cooldownUntil || !hasMedallion(player)) return;
        cooldownUntil = clientTick + COOLDOWN_TICKS;
        ClientPacketDistributor.sendToServer(new SpecterDodgePayload());
        player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 3.0F, 1.0F);
    }

    private static boolean hasMedallion(Player player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (AbilityItems.hasMedallionAbility(stack, GeneratedMod.SPECTER_MEDALLION.get())) {
                return true;
            }
        }
        // 26.1.2：LivingEntity#getArmorSlots() 已移除，改为按装备槽查询。
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET}) {
            if (AbilityItems.hasMedallionAbility(player.getItemBySlot(slot), GeneratedMod.SPECTER_MEDALLION.get())) {
                return true;
            }
        }
        return AbilityItems.hasMedallionAbility(player.getOffhandItem(), GeneratedMod.SPECTER_MEDALLION.get());
    }
}
