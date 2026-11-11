package cn.blockforge.generated.origoldsar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 无线红石的持久化数据：金源无线红石连接装置的线路表，以及金源无线红石遥控开关
 * 的远程信号发射器表。保存在服务器主世界（overworld）的存档数据里，键名
 * {@code origolds_ar_wireless_redstone}。
 *
 * <p>每条线路由 {@code a -> b} 构成：服务端每 tick 读取 a 位置的红石信号强度，
 * 并把它作为 b 位置的“虚拟电源”输出（弱信号 + 强信号，见 {@link WirelessRedstoneManager}
 * 与 mixin）。发射器是遥控开关的远程拉杆输出，输出固定 15 级。
 */
public final class WirelessRedstoneData extends SavedData {
    private static final String DATA_NAME = "origolds_ar_wireless_redstone";

    /** 一条无线红石线路。 */
    public record WirelessLink(UUID id, ResourceKey<Level> dimA, BlockPos posA, ResourceKey<Level> dimB, BlockPos posB) {}

    /** 遥控开关的一个远程信号发射器。 */
    public record SwitchEmitter(UUID id, ResourceKey<Level> dim, BlockPos pos, boolean active) {}

    private final List<WirelessLink> links = new ArrayList<>();
    private final Map<UUID, SwitchEmitter> emitters = new LinkedHashMap<>();

    public static WirelessRedstoneData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new Factory<>(WirelessRedstoneData::new, WirelessRedstoneData::load), DATA_NAME);
    }

    private static WirelessRedstoneData load(CompoundTag tag, HolderLookup.Provider registries) {
        WirelessRedstoneData data = new WirelessRedstoneData();
        ListTag linkList = tag.getList("links", Tag.TAG_COMPOUND);
        for (int i = 0; i < linkList.size(); i++) {
            CompoundTag t = linkList.getCompound(i);
            data.links.add(new WirelessLink(
                    t.getUUID("id"),
                    dim(t.getString("adim")),
                    fromLong(t.getLong("apos")),
                    dim(t.getString("bdim")),
                    fromLong(t.getLong("bpos"))));
        }
        ListTag emitterList = tag.getList("emitters", Tag.TAG_COMPOUND);
        for (int i = 0; i < emitterList.size(); i++) {
            CompoundTag t = emitterList.getCompound(i);
            SwitchEmitter e = new SwitchEmitter(
                    t.getUUID("id"),
                    dim(t.getString("dim")),
                    fromLong(t.getLong("pos")),
                    t.getBoolean("on"));
            data.emitters.put(e.id(), e);
        }
        return data;
    }

    /** 把 {@link BlockPos#asLong()} 打包的坐标还原为不可变 BlockPos。
     *  注意：1.20.5 起打包位序改为 x:38..63 / z:12..37 / y:0..11（y 在低位），
     *  绝不能再按旧版位序手工拆位，否则解出的坐标面目全非——必须用官方的
     *  {@link BlockPos#of(long)} 做 {@code asLong()} 的精确逆运算。 */
    public static BlockPos fromLong(long packed) {
        return BlockPos.of(packed);
    }

    private static ResourceKey<Level> dim(String name) {
        return dimKey(ResourceLocation.parse(name));
    }

    public static ResourceKey<Level> dimKey(ResourceLocation location) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag linkList = new ListTag();
        for (WirelessLink link : links) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", link.id());
            t.putString("adim", link.dimA().location().toString());
            t.putLong("apos", link.posA().asLong());
            t.putString("bdim", link.dimB().location().toString());
            t.putLong("bpos", link.posB().asLong());
            linkList.add(t);
        }
        tag.put("links", linkList);
        ListTag emitterList = new ListTag();
        for (SwitchEmitter e : emitters.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", e.id());
            t.putString("dim", e.dim().location().toString());
            t.putLong("pos", e.pos().asLong());
            t.putBoolean("on", e.active());
            emitterList.add(t);
        }
        tag.put("emitters", emitterList);
        return tag;
    }

    // ------------------------------------------------------------------ 线路

    public List<WirelessLink> links() {
        return links;
    }

    public void addLink(WirelessLink link) {
        links.add(link);
        setDirty();
    }

    public boolean removeLink(UUID id) {
        boolean removed = links.removeIf(link -> link.id().equals(id));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    /** 按 id 查找一条线路，没有则返回 null。 */
    public WirelessLink link(UUID id) {
        for (WirelessLink link : links) {
            if (link.id().equals(id)) {
                return link;
            }
        }
        return null;
    }

    /** 原地反转某条线路的方向（交换输入端与输出端），返回是否反转成功。 */
    public boolean reverseLink(UUID id) {
        for (int i = 0; i < links.size(); i++) {
            WirelessLink link = links.get(i);
            if (link.id().equals(id)) {
                links.set(i, new WirelessLink(link.id(), link.dimB(), link.posB(), link.dimA(), link.posA()));
                setDirty();
                return true;
            }
        }
        return false;
    }

    /** 是否已经存在一条端点完全相同的无线线路（方向相关：a→b 与 b→a 视为两条）。 */
    public boolean hasLink(ResourceKey<Level> dimA, BlockPos posA, ResourceKey<Level> dimB, BlockPos posB) {
        for (WirelessLink link : links) {
            if (link.dimA().equals(dimA) && link.posA().equals(posA)
                    && link.dimB().equals(dimB) && link.posB().equals(posB)) {
                return true;
            }
        }
        return false;
    }

    /** 仅在端点完全相同的线路尚不存在时加入，返回是否真正新增（用于结构方块放置时的去重）。 */
    public boolean addLinkIfAbsent(WirelessLink link) {
        if (hasLink(link.dimA(), link.posA(), link.dimB(), link.posB())) {
            return false;
        }
        links.add(link);
        setDirty();
        return true;
    }

    // ------------------------------------------------------------------ 发射器

    public java.util.Collection<SwitchEmitter> emitters() {
        return emitters.values();
    }

    public SwitchEmitter emitter(UUID id) {
        return emitters.get(id);
    }

    public void putEmitter(SwitchEmitter emitter) {
        emitters.put(emitter.id(), emitter);
        setDirty();
    }

    public void removeEmitter(UUID id) {
        if (emitters.remove(id) != null) {
            setDirty();
        }
    }
}
