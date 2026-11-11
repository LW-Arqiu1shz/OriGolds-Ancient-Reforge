package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 -> 服务端：钩爪弩炮的点击操作。
 * {@code action} 0 = 左键（拉人 / 潜行时拉生物），1 = 中键（清除绑定），
 * 2 = 潜行 + 右键（中断当前拖拽但保留与箭矢的绑定）。
 */
public record GrapnelActionPayload(byte action) implements CustomPacketPayload {
    public static final byte ACTION_LEFT_CLICK = 0;
    public static final byte ACTION_CLEAR = 1;
    /** 潜行 + 右键：立刻取消当前动作（如正在被拉过去的途中“下车”），但不解除与箭矢的绑定。 */
    public static final byte ACTION_DROP = 2;

    public static final Type<GrapnelActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "grapnel_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GrapnelActionPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public GrapnelActionPayload decode(RegistryFriendlyByteBuf buf) {
                    return new GrapnelActionPayload(buf.readByte());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, GrapnelActionPayload payload) {
                    buf.writeByte(payload.action());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
