package cn.blockforge.generated.origoldsar;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 钩爪弩炮操作包的注册与处理。 */
public final class GrapnelNetwork {
    private static final String PREVIEW_ON = "message.origolds_ar.grapnel_preview_on";
    private static final String PREVIEW_OFF = "message.origolds_ar.grapnel_preview_off";

    private GrapnelNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(
                GrapnelActionPayload.TYPE,
                GrapnelActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    if (!holdsBallista(player)) {
                        return;
                    }
                    if (payload.action() == GrapnelActionPayload.ACTION_CLEAR) {
                        // 潜行 + 中键：立刻清除绑定并中断所有拖拽。
                        GrapnelManager.breakBinding(player);
                        message(player, "message.origolds_ar.grapnel_clear");
                    } else if (payload.action() == GrapnelActionPayload.ACTION_DROP) {
                        // 潜行 + 右键：立刻取消当前动作（“下车”），但保留与箭矢的绑定。
                        GrapnelManager.dropPull(player);
                        message(player, "message.origolds_ar.grapnel_drop");
                    } else {
                        GrapnelManager.onLeftClick(player, player.isShiftKeyDown());
                    }
                }));

        registrar.playToServer(
                GrapnelPreviewTogglePayload.TYPE,
                GrapnelPreviewTogglePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        GrapnelManager.setPreviewEnabled(player.getUUID(), payload.enabled());
                        // 1.2.0 起开关反馈由服务端统一走信息栏（客户端不再本地弹字）。
                        PlayerInfoBar.show(player, Component.translatable(
                                payload.enabled() ? PREVIEW_ON : PREVIEW_OFF));
                    }
                }));

        registrar.playToClient(
                GrapnelPreviewPayload.TYPE,
                GrapnelPreviewPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.blockforge.generated.origoldsar.client.GrapnelClient.onPreviewSync(
                                payload.hasTarget(), payload.x(), payload.y(), payload.z())));
    }

    private static boolean holdsBallista(ServerPlayer player) {
        return player.getMainHandItem().is(GeneratedMod.GRAPNEL_BALLISTA.get())
                || player.getOffhandItem().is(GeneratedMod.GRAPNEL_BALLISTA.get());
    }

    private static void message(ServerPlayer player, String key) {
        PlayerInfoBar.show(player, Component.translatable(key));
    }
}
