package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * 服务端 -> 客户端：同步某个玩家的“待选中转点”（已选了一个坐标点、还差第二个点）。
 *
 * <p>客户端据此在手持连接装置时给该点画一个呼吸灯式高亮框。当 {@code present} 为
 * {@code false} 时表示当前没有待选中的点（两点的线路已建成或已清空），客户端清空显示。
 */
public record WirelessSelectSyncPayload(boolean present, ResourceKey<Level> dim, long pos, boolean start)
        implements CustomPacketPayload {

    public static final Type<WirelessSelectSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "wireless_select_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WirelessSelectSyncPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public WirelessSelectSyncPayload decode(RegistryFriendlyByteBuf buf) {
                    boolean present = buf.readBoolean();
                    if (!present) {
                        return new WirelessSelectSyncPayload(false, null, 0L, false);
                    }
                    ResourceKey<Level> dim = WirelessRedstoneData.dimKey(buf.readResourceLocation());
                    long pos = buf.readLong();
                    boolean start = buf.readBoolean();
                    return new WirelessSelectSyncPayload(true, dim, pos, start);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, WirelessSelectSyncPayload payload) {
                    buf.writeBoolean(payload.present());
                    if (payload.present()) {
                        buf.writeResourceLocation(payload.dim().location());
                        buf.writeLong(payload.pos());
                        buf.writeBoolean(payload.start());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
