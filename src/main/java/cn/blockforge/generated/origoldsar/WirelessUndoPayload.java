package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：按下 Ctrl+Z（撤销）或 Ctrl+Y（重做）。
 *
 * <p>服务端按当前手持物品决定作用于哪支历史栈：手持某个遥控开关时只撤销 / 重做
 * <b>这一把</b>开关自己的绑定 / 解绑操作（每把开关独立保存）；手持任意连接装置时
 * 撤销 / 重做无线线路相关操作（新增 / 删除连线、反转输入输出方向）。
 */
public record WirelessUndoPayload(boolean undo) implements CustomPacketPayload {
    public static final Type<WirelessUndoPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "wireless_undo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WirelessUndoPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public WirelessUndoPayload decode(RegistryFriendlyByteBuf buf) {
                    return new WirelessUndoPayload(buf.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, WirelessUndoPayload payload) {
                    buf.writeBoolean(payload.undo());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
