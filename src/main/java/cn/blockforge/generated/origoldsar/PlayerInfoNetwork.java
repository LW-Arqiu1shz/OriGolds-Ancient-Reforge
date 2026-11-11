package cn.blockforge.generated.origoldsar;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** {@link PlayerInfoPayload} 的服务端注册与处理（MOD 总线，由 GeneratedMod 显式挂载）。 */
public final class PlayerInfoNetwork {
    private PlayerInfoNetwork() {}

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
