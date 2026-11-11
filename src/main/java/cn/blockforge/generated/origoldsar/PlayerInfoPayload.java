package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：请求在玩家信息栏里显示一条提示（翻译键，无参数）。
 *
 * <p>本模组有些提示是纯客户端触发的（切换显示模式、切换路径预览），而父模组的玩家信息栏
 * 接口在服务端结算，因此统一由服务端代为显示，这样多人游戏里也能正常进信息栏。
 */
public record PlayerInfoPayload(String translationKey) implements CustomPacketPayload {
    public static final Type<PlayerInfoPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "player_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerInfoPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PlayerInfoPayload decode(RegistryFriendlyByteBuf buf) {
                    return new PlayerInfoPayload(buf.readUtf());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, PlayerInfoPayload payload) {
                    buf.writeUtf(payload.translationKey());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
