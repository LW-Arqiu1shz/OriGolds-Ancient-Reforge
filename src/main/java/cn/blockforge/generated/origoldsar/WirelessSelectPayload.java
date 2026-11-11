package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：金源无线红石连接装置潜行 + 左键 / 潜行 + 右键命中一个方块，
 * 把它选为线路起点（{@code start}=true）或终点（{@code start}=false）。
 * 坐标打包自 {@code BlockPos#asLong()}，维度取玩家当前维度。射程在客户端按 32 格射线计算。
 */
public record WirelessSelectPayload(long pos, boolean start) implements CustomPacketPayload {
    public static final Type<WirelessSelectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "wireless_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WirelessSelectPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public WirelessSelectPayload decode(RegistryFriendlyByteBuf buf) {
                    return new WirelessSelectPayload(buf.readLong(), buf.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, WirelessSelectPayload payload) {
                    buf.writeLong(payload.pos());
                    buf.writeBoolean(payload.start());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
