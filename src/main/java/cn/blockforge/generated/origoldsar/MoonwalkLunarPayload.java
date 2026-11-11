package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端在“按住潜行键 + 双击月行键”时发给服务端，请求开启/关闭持有者的月球重力。
 * 月球重力激活时，重力降为原来的 25%，最大终端下落速度降为原来的 40%。
 */
public record MoonwalkLunarPayload() implements CustomPacketPayload {
    public static final Type<MoonwalkLunarPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "moonwalk_lunar"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoonwalkLunarPayload> STREAM_CODEC =
            StreamCodec.unit(new MoonwalkLunarPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
