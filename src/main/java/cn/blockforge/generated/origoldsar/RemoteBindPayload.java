package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 -> 服务端：金源无线红石遥控开关左键命中一个方块，把物品绑定到它
 * （坐标打包自 {@code BlockPos#asLong()}，维度取玩家当前维度）。
 */
public record RemoteBindPayload(long pos) implements CustomPacketPayload {
    public static final Type<RemoteBindPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "remote_bind"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteBindPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public RemoteBindPayload decode(RegistryFriendlyByteBuf buf) {
                    return new RemoteBindPayload(buf.readLong());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, RemoteBindPayload payload) {
                    buf.writeLong(payload.pos());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
