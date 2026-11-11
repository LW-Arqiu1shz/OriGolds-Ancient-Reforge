package cn.blockforge.generated.origoldsar;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 1.21.1 旧存档 → 26.x 的无线红石数据迁移。
 *
 * <p>1.21.1 把线路表与遥控开关发射器存在 {@code data/origolds_ar_wireless_redstone.dat}
 * （手写 NBT，UUID 是 int 数组）；26.x 改用 {@code SavedDataType}（{@code origolds_ar:wireless_redstone}），
 * 文件路径和格式都变了。把 1.21.1 的世界升级到 26.x 后，所有无线线路和遥控开关输出会悄无声息地消失。</p>
 *
 * <p>开服完成时检查一次：新数据为空、且能找到旧文件时导入，然后把旧文件改名为 {@code *.migrated}，
 * 以后不会重复导入。新数据已经有内容时不碰旧文件。任何异常只记日志，不影响开服。</p>
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class WirelessLegacyMigration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LEGACY_FILE = "origolds_ar_wireless_redstone.dat";

    private WirelessLegacyMigration() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        try {
            migrate(event.getServer());
        } catch (Exception e) {
            LOGGER.warn("[origolds_ar] 旧版无线红石数据迁移失败，已跳过", e);
        }
    }

    private static void migrate(MinecraftServer server) throws Exception {
        Path file = findLegacy(server);
        if (file == null) {
            return;
        }
        WirelessRedstoneData data = WirelessRedstoneData.get(server);
        if (!data.links().isEmpty() || !data.emitters().isEmpty()) {
            return;
        }
        CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        CompoundTag body = root.getCompound("data").orElse(root);

        int links = 0;
        for (CompoundTag t : compounds(body, "links")) {
            Optional<UUID> id = t.read("id", UUIDUtil.LENIENT_CODEC);
            ResourceKey<Level> dimA = dim(t, "adim");
            ResourceKey<Level> dimB = dim(t, "bdim");
            Optional<Long> posA = t.getLong("apos");
            Optional<Long> posB = t.getLong("bpos");
            if (id.isEmpty() || dimA == null || dimB == null || posA.isEmpty() || posB.isEmpty()) {
                continue;
            }
            data.addLinkIfAbsent(new WirelessRedstoneData.WirelessLink(
                    id.get(), dimA, BlockPos.of(posA.get()), dimB, BlockPos.of(posB.get())));
            links++;
        }

        int emitters = 0;
        for (CompoundTag t : compounds(body, "emitters")) {
            Optional<UUID> id = t.read("id", UUIDUtil.LENIENT_CODEC);
            ResourceKey<Level> dim = dim(t, "dim");
            Optional<Long> pos = t.getLong("pos");
            if (id.isEmpty() || dim == null || pos.isEmpty()) {
                continue;
            }
            data.putEmitter(new WirelessRedstoneData.SwitchEmitter(
                    id.get(), dim, BlockPos.of(pos.get()), t.getBooleanOr("on", false)));
            emitters++;
        }

        Files.move(file, file.resolveSibling(file.getFileName() + ".migrated"));
        LOGGER.info("[origolds_ar] 已从 1.21.1 旧存档导入 {} 条无线线路、{} 个遥控开关输出（{}）",
                links, emitters, file);
    }

    /** 旧文件可能还在世界根的 data/ 里，也可能被原版的存档升级挪进了主世界的维度目录。 */
    private static Path findLegacy(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        for (Path candidate : List.of(
                root.resolve("data").resolve(LEGACY_FILE),
                root.resolve("dimensions").resolve("minecraft").resolve("overworld")
                        .resolve("data").resolve(LEGACY_FILE))) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static List<CompoundTag> compounds(CompoundTag body, String key) {
        ListTag list = body.getListOrEmpty(key);
        List<CompoundTag> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            list.getCompound(i).ifPresent(out::add);
        }
        return out;
    }

    private static ResourceKey<Level> dim(CompoundTag tag, String key) {
        Identifier id = tag.getString(key).map(Identifier::tryParse).orElse(null);
        return id == null ? null : WirelessRedstoneData.dimKey(id);
    }
}
