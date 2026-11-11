package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端在“按住月行键 + 双击潜行键”时发给服务端，请求反转/恢复持有者的重力方向。
 */
public record MoonwalkTogglePayload() implements CustomPacketPayload {
    public static final Type<MoonwalkTogglePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "moonwalk_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoonwalkTogglePayload> STREAM_CODEC =
            StreamCodec.unit(new MoonwalkTogglePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
