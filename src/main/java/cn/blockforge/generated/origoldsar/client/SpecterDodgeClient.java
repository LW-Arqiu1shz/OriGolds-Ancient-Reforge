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
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
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
        PacketDistributor.sendToServer(new SpecterDodgePayload());
        player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 3.0F, 1.0F);
    }

    private static boolean hasMedallion(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (AbilityItems.hasMedallionAbility(stack, GeneratedMod.SPECTER_MEDALLION.get())) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (AbilityItems.hasMedallionAbility(stack, GeneratedMod.SPECTER_MEDALLION.get())) {
                return true;
            }
        }
        return AbilityItems.hasMedallionAbility(player.getOffhandItem(), GeneratedMod.SPECTER_MEDALLION.get());
    }
}
