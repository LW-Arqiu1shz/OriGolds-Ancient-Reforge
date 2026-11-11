package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 客户端 -> 服务端：在无线红石显示模式下按 G（反转），反转准星悬停的这条无线红石线路的方向
 * （交换输入端 A 与输出端 B，让信号往反方向流动）。
 */
public record WirelessReversePayload(UUID linkId) implements CustomPacketPayload {
    public static final Type<WirelessReversePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "wireless_reverse"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WirelessReversePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public WirelessReversePayload decode(RegistryFriendlyByteBuf buf) {
                    return new WirelessReversePayload(buf.readUUID());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, WirelessReversePayload payload) {
                    buf.writeUUID(payload.linkId());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
