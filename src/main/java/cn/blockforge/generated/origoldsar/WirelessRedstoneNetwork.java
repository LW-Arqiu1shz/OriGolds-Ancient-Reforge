package cn.blockforge.generated.origoldsar;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 无线红石连接装置 / 遥控开关的网络包注册与处理。 */
public final class WirelessRedstoneNetwork {
    private WirelessRedstoneNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // 客户端 -> 服务端：连接装置潜行左键选起点 / 潜行右键选终点
        registrar.playToServer(WirelessSelectPayload.TYPE, WirelessSelectPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        WirelessRedstoneManager.onSelect(player, payload.pos(), payload.start());
                    }
                }));

        // 客户端 -> 服务端：显示模式下按 Delete 删除线路
        registrar.playToServer(WirelessDeletePayload.TYPE, WirelessDeletePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        WirelessRedstoneManager.onDelete(player, payload.linkId());
                    }
                }));

        // 客户端 -> 服务端：显示模式下按 Backspace 反转线路方向
        registrar.playToServer(WirelessReversePayload.TYPE, WirelessReversePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        WirelessRedstoneManager.onReverse(player, payload.linkId());
                    }
                }));

        // 客户端 -> 服务端：遥控开关左键绑定方块
        registrar.playToServer(RemoteBindPayload.TYPE, RemoteBindPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        WirelessRedstoneManager.onBind(player, payload.pos());
                    }
                }));

        // 客户端 -> 服务端：遥控开关对准已绑定方块按 Delete 解除绑定
        registrar.playToServer(RemoteUnbindPayload.TYPE, RemoteUnbindPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        WirelessRedstoneManager.onUnbind(player, payload.pos());
                    }
                }));

        // 客户端 -> 服务端：Ctrl+Z 撤销 / Ctrl+Y 重做（按手持选线路历史或该开关自己的历史）
        registrar.playToServer(WirelessUndoPayload.TYPE, WirelessUndoPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        WirelessRedstoneManager.onUndoRedo(player, payload.undo());
                    }
                }));

        // 服务端 -> 客户端：全量同步线路表
        registrar.playToClient(WirelessLinkSyncPayload.TYPE, WirelessLinkSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.blockforge.generated.origoldsar.client.WirelessConnectorClient.onLinkSync(payload)));

        // 服务端 -> 客户端：同步“待选中转点”（已选一个点、还差第二个点时画呼吸高亮框）
        registrar.playToClient(WirelessSelectSyncPayload.TYPE, WirelessSelectSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.blockforge.generated.origoldsar.client.WirelessConnectorClient.onSelectSync(payload)));
    }
}
