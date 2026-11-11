package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 客户端 -> 服务端：在无线红石显示模式下按 Delete，删除准星悬停的这条无线红石线路。
 */
public record WirelessDeletePayload(UUID linkId) implements CustomPacketPayload {
    public static final Type<WirelessDeletePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "wireless_delete"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WirelessDeletePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public WirelessDeletePayload decode(RegistryFriendlyByteBuf buf) {
                    return new WirelessDeletePayload(buf.readUUID());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, WirelessDeletePayload payload) {
                    buf.writeUUID(payload.linkId());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
