package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 -> 客户端：把“当前钩爪目标”的位置流式同步给开启了路径预览的玩家。
 * {@code hasTarget} 为 {@code false} 表示当前没有可预览的目标（未绑定/已失效），
 * 客户端据此清掉本地保存的预览终点。
 */
public record GrapnelPreviewPayload(boolean hasTarget, double x, double y, double z) implements CustomPacketPayload {
    public static final Type<GrapnelPreviewPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "grapnel_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GrapnelPreviewPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public GrapnelPreviewPayload decode(RegistryFriendlyByteBuf buf) {
                    return new GrapnelPreviewPayload(buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, GrapnelPreviewPayload payload) {
                    buf.writeBoolean(payload.hasTarget());
                    buf.writeDouble(payload.x());
                    buf.writeDouble(payload.y());
                    buf.writeDouble(payload.z());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
