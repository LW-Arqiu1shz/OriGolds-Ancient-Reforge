package cn.blockforge.generated.origoldsar;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 自循环附魔之瓶的 Ctrl + 右键（消耗全部经验）操作包。 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class SelfBottleNetwork {
    private SelfBottleNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                SelfBottlePayload.TYPE,
                SelfBottlePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && holdsBottle(player)) {
                        SelfCirculatingBottleItem.throwAllExperience(player.level(), player);
                    }
                }));
    }

    private static boolean holdsBottle(ServerPlayer player) {
        return player.getMainHandItem().is(GeneratedMod.SELF_CIRCULATING_BOTTLE.get())
                || player.getOffhandItem().is(GeneratedMod.SELF_CIRCULATING_BOTTLE.get());
    }
}
