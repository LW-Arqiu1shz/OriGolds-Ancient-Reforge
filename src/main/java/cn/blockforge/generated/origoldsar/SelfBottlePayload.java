package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：请求以 Ctrl + 右键的方式投掷自循环附魔之瓶（消耗全部经验）。
 *
 * <p>Ctrl 是原版不会随右键同步到服务端的修饰键，因此由客户端探测后发送本包，
 * 服务端收到后校验玩家确实手持该物品再结算。
 */
public record SelfBottlePayload() implements CustomPacketPayload {
    public static final Type<SelfBottlePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "self_bottle_throw_all"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelfBottlePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SelfBottlePayload decode(RegistryFriendlyByteBuf buf) {
                    return new SelfBottlePayload();
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SelfBottlePayload payload) {
                    // 无字段。
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
