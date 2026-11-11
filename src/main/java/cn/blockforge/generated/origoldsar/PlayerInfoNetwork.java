package cn.blockforge.generated.origoldsar;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 客户端触发的玩家信息栏提示包：服务端收到后交给 {@link PlayerInfoBar} 显示。 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class PlayerInfoNetwork {
    private PlayerInfoNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                PlayerInfoPayload.TYPE,
                PlayerInfoPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        PlayerInfoBar.show(player, Component.translatable(payload.translationKey()));
                    }
                }));
    }
}
