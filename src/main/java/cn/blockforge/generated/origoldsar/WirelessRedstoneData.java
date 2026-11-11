package cn.blockforge.generated.origoldsar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 无线红石的持久化数据：金源无线红石连接装置的线路表，以及金源无线红石遥控开关
 * 的远程信号发射器表。保存在服务器主世界的存档数据里，键名
 * {@code origolds_ar:wireless_redstone}。
 *
 * <p>每条线路由 {@code a -> b} 构成：服务端每 tick 读取 a 位置的红石信号强度，
 * 并把它作为 b 位置的“虚拟电源”输出（弱信号 + 强信号，见 {@link WirelessRedstoneManager}
 * 与 mixin）。发射器是遥控开关的远程拉杆输出，输出固定 15 级。
 *
 * <p>26.1.2 起存档数据不再是手写 NBT，而是给整份数据写一个 {@link Codec}，
 * 再由 {@link SavedDataType} 负责落盘。
 */
public final class WirelessRedstoneData extends SavedData {
    /** 世界维度的序列化键（与 {@code Level.RESOURCE_KEY_CODEC} 等价）。 */
    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(net.minecraft.core.registries.Registries.DIMENSION);
    /**
     * 写成字符串（与已有 26.x 存档一致），读时也认 int 数组。
     * 以前用 {@code xmap(UUID::fromString)}：一条坏 id 会直接抛异常，整份数据读取失败，
     * 存档系统随后会新建一份空数据并在下次保存时覆盖原文件——所有线路一起丢失。
     * 现在坏条目只是解码错误，被列表解码跳过，其它线路照常保留。
     */
    private static final Codec<UUID> UUID_CODEC = Codec.withAlternative(
            net.minecraft.core.UUIDUtil.STRING_CODEC, net.minecraft.core.UUIDUtil.CODEC);
    private static final Codec<BlockPos> POS_CODEC = Codec.LONG.xmap(BlockPos::of, BlockPos::asLong);

    /** 一条无线红石线路。 */
    public record WirelessLink(UUID id, ResourceKey<Level> dimA, BlockPos posA, ResourceKey<Level> dimB, BlockPos posB) {
        public static final Codec<WirelessLink> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUID_CODEC.fieldOf("id").forGetter(WirelessLink::id),
                DIMENSION_CODEC.fieldOf("adim").forGetter(WirelessLink::dimA),
                POS_CODEC.fieldOf("apos").forGetter(WirelessLink::posA),
                DIMENSION_CODEC.fieldOf("bdim").forGetter(WirelessLink::dimB),
                POS_CODEC.fieldOf("bpos").forGetter(WirelessLink::posB)
        ).apply(i, WirelessLink::new));
    }

    /** 遥控开关的一个远程信号发射器。 */
    public record SwitchEmitter(UUID id, ResourceKey<Level> dim, BlockPos pos, boolean active) {
        public static final Codec<SwitchEmitter> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUID_CODEC.fieldOf("id").forGetter(SwitchEmitter::id),
                DIMENSION_CODEC.fieldOf("dim").forGetter(SwitchEmitter::dim),
                POS_CODEC.fieldOf("pos").forGetter(SwitchEmitter::pos),
                Codec.BOOL.optionalFieldOf("on", false).forGetter(SwitchEmitter::active)
        ).apply(i, SwitchEmitter::new));
    }

    public static final Codec<WirelessRedstoneData> CODEC = RecordCodecBuilder.create(i -> i.group(
            WirelessLink.CODEC.listOf().optionalFieldOf("links", List.of()).forGetter(data -> List.copyOf(data.links)),
            SwitchEmitter.CODEC.listOf().optionalFieldOf("emitters", List.of()).forGetter(data -> List.copyOf(data.emitters.values()))
    ).apply(i, WirelessRedstoneData::new));

    /**
     * 不挂数据修复器（NeoForge 允许为 null）：以前传的 {@code DataFixTypes.LEVEL} 是给 level.dat
     * 用的修复规则，套在这份线路数据上毫无意义，版本号对不上时还会拿主世界规则去“修”它。
     */
    public static final SavedDataType<WirelessRedstoneData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "wireless_redstone"),
            WirelessRedstoneData::new, CODEC);

    private final List<WirelessLink> links = new ArrayList<>();
    private final Map<UUID, SwitchEmitter> emitters = new LinkedHashMap<>();

    public WirelessRedstoneData() {
    }

    public WirelessRedstoneData(List<WirelessLink> links, List<SwitchEmitter> emitters) {
        this.links.addAll(links);
        for (SwitchEmitter emitter : emitters) {
            this.emitters.put(emitter.id(), emitter);
        }
    }

    public static WirelessRedstoneData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** 把 {@link BlockPos#asLong()} 打包的坐标还原为不可变 BlockPos。
     *  注意：1.20.5 起打包位序改为 x:38..63 / z:12..37 / y:0..11（y 在低位），
     *  绝不能再按旧版位序手工拆位，否则解出的坐标面目全非——必须用官方的
     *  {@link BlockPos#of(long)} 做 {@code asLong()} 的精确逆运算。 */
    public static BlockPos fromLong(long packed) {
        return BlockPos.of(packed);
    }

    public static ResourceKey<Level> dimKey(Identifier location) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
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
