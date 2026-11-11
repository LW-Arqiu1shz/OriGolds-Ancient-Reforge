package cn.blockforge.generated.origoldsar;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * {@link SelfBottlePayload} 的服务端注册与处理（MOD 总线，由 GeneratedMod 显式挂载）：
 * 收到“扔光经验”请求后复核持瓶状态，再调用 {@link SelfCirculatingBottleItem#throwAllExperience}。
 */
public final class SelfBottleNetwork {
    private SelfBottleNetwork() {}

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
