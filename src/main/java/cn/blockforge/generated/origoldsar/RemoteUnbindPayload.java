package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：手持遥控开关、准星对着已绑定的方块按下 Delete，
 * 请求解除该绑定（同时移除对应的远程信号发射器）。
 * 坐标打包自 {@code BlockPos#asLong()}，服务端据此校验确实是当前绑定的方块且在 32 格射程内。
 */
public record RemoteUnbindPayload(long pos) implements CustomPacketPayload {
    public static final Type<RemoteUnbindPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "remote_unbind"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteUnbindPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public RemoteUnbindPayload decode(RegistryFriendlyByteBuf buf) {
                    return new RemoteUnbindPayload(buf.readLong());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, RemoteUnbindPayload payload) {
                    buf.writeLong(payload.pos());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
