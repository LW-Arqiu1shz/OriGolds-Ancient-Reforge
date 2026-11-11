package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 -> 服务端：Ctrl + 右键请求把玩家身上全部经验（上限一只瓶子的装载量）
 * 装进自循环附魔之瓶掷出（1.2.0 新功能，见 {@link SelfCirculatingBottleItem}）。
 * 空载荷，纯触发信号；是否手持瓶子、经验是否够由服务端复核。
 */
public record SelfBottlePayload() implements CustomPacketPayload {
    public static final Type<SelfBottlePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "self_bottle_throw_all"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelfBottlePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SelfBottlePayload decode(RegistryFriendlyByteBuf buf) {
                    return new SelfBottlePayload();
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SelfBottlePayload payload) {
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
