package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 -> 服务端：请求把一条翻译键提示展示到玩家信息栏（1.2.0 新功能）。
 * 显示模式开关这类反馈原本在客户端本地刷 action bar；改走服务端后
 * 统一由 {@link PlayerInfoBar} 决定是进基模信息栏还是回退 action bar。
 */
public record PlayerInfoPayload(String translationKey) implements CustomPacketPayload {
    public static final Type<PlayerInfoPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "player_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerInfoPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PlayerInfoPayload decode(RegistryFriendlyByteBuf buf) {
                    return new PlayerInfoPayload(buf.readUtf(128));
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
