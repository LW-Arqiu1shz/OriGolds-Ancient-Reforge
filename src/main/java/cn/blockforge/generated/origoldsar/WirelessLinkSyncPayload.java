package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 服务端 -> 客户端：全量同步无线红石线路表（建立/删除线路与玩家进服时发送），
 * 供手持连接装置开启无线红石显示模式时渲染粒子线、悬停与删除。
 */
public record WirelessLinkSyncPayload(List<LinkView> links) implements CustomPacketPayload {
    /** 单条线路的视图数据。 */
    public record LinkView(UUID id, ResourceKey<Level> dimA, long posA, ResourceKey<Level> dimB, long posB) {}

    public static final Type<WirelessLinkSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "wireless_link_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WirelessLinkSyncPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public WirelessLinkSyncPayload decode(RegistryFriendlyByteBuf buf) {
                    int size = buf.readVarInt();
                    List<LinkView> links = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        UUID id = buf.readUUID();
                        ResourceKey<Level> dimA = WirelessRedstoneData.dimKey(buf.readIdentifier());
                        long posA = buf.readLong();
                        ResourceKey<Level> dimB = WirelessRedstoneData.dimKey(buf.readIdentifier());
                        long posB = buf.readLong();
                        links.add(new LinkView(id, dimA, posA, dimB, posB));
                    }
                    return new WirelessLinkSyncPayload(links);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, WirelessLinkSyncPayload payload) {
                    buf.writeVarInt(payload.links().size());
                    for (LinkView link : payload.links()) {
                        buf.writeUUID(link.id());
                        buf.writeIdentifier(link.dimA().identifier());
                        buf.writeLong(link.posA());
                        buf.writeIdentifier(link.dimB().identifier());
                        buf.writeLong(link.posB());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
