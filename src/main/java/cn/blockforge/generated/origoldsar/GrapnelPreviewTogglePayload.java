package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：切换“路径预览”开关（手持钩爪弩炮时 Ctrl + 中键触发）。
 * 服务端据此决定是否持续把当前钩爪目标的位置流式同步给该玩家。
 */
public record GrapnelPreviewTogglePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<GrapnelPreviewTogglePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "grapnel_preview_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GrapnelPreviewTogglePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public GrapnelPreviewTogglePayload decode(RegistryFriendlyByteBuf buf) {
                    return new GrapnelPreviewTogglePayload(buf.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, GrapnelPreviewTogglePayload payload) {
                    buf.writeBoolean(payload.enabled());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
