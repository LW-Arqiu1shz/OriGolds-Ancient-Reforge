package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端显示模式的<b>跨会话记忆</b>：记住设备退出游戏前处于什么模式、该模式是开还是关，
 * 下次启动游戏时原样恢复。
 *
 * <p>状态存于 {@code config/origolds_ar_display_modes.json}：
 * <pre>{@code
 * {
 *   "current": "origolds_ar:wireless_redstone",
 *   "modes": { "origolds_ar:wireless_redstone": true, "origolds_ar:remote_switch": false }
 * }
 * }</pre>
 *
 * <p><b>模式以字符串 id 为键</b>，不固定枚举——本模组的附属模组给连接装置加入新显示模式时，
 * 只需使用自己命名空间的模式 id（例如 {@code "addonid:mode_name"}）调用本类的
 * {@link #toggle(String)} / {@link #setOn(String, boolean)} 切换模式，并在自身初始化时
 * 用 {@link #isOn(String)} 读取上次的开关状态（{@link #currentMode()} 可查退出游戏前
 * 最后处于哪个模式），即可免费获得同样的持久化记忆，无需改动本模组。
 *
 * <p>每次状态变更都立即落盘（写文件失败只记日志、不影响游戏内切换），因此即使游戏崩溃，
 * 最后一次的模式状态也已被记住。所有调用都发生在客户端主线程。
 */
public final class DisplayModeMemory {
    /** 金源无线连接装置：无线红石显示模式。 */
    public static final String MODE_WIRELESS = GeneratedMod.MOD_ID + ":wireless_redstone";
    /** 金源无线遥控开关：遥控显示模式。 */
    public static final String MODE_REMOTE = GeneratedMod.MOD_ID + ":remote_switch";

    private static final Logger LOGGER = LoggerFactory.getLogger("origolds_ar/DisplayModeMemory");
    private static final Path FILE = FMLPaths.CONFIGDIR.get()
            .resolve(GeneratedMod.MOD_ID + "_display_modes.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 模式 id -> 开/关。 */
    private static final Map<String, Boolean> modes = new HashMap<>();
    /** 退出游戏前设备最后处于的模式 id（附属的新模式同样记录在这里）。 */
    private static String current = MODE_WIRELESS;
    /** 是否已从磁盘加载过一次（懒加载，首次访问时读文件）。 */
    private static boolean loaded = false;

    private DisplayModeMemory() {}

    /** 查询某显示模式上次退出游戏时是开还是关；未知模式按关处理。 */
    public static synchronized boolean isOn(String mode) {
        ensureLoaded();
        return Boolean.TRUE.equals(modes.get(mode));
    }

    /**
     * 设置某显示模式的开关并立即落盘，同时把它记为“当前所处的模式”。
     *
     * <p>无线红石显示模式额外同步 {@link WirelessConnectorClient#connectorDisplay} 实时字段——
     * 附属模组（origolds_ia 的三模式互斥桥）反射读写的就是那个字段，持久化状态变更必须让它跟上。
     */
    public static synchronized void setOn(String mode, boolean on) {
        ensureLoaded();
        modes.put(mode, on);
        current = mode;
        if (MODE_WIRELESS.equals(mode)) {
            WirelessConnectorClient.connectorDisplay = on;
        }
        save();
    }

    /** 切换某显示模式的开关（开↔关），返回切换后的新状态。 */
    public static synchronized boolean toggle(String mode) {
        boolean on = !isOn(mode);
        setOn(mode, on);
        return on;
    }

    /** 退出游戏前设备最后处于的模式 id（供本模组与附属在启动时对齐状态）。 */
    public static synchronized String currentMode() {
        ensureLoaded();
        return current;
    }

    // ---------------------------------------------------------------- 落盘

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            if (!Files.exists(FILE)) {
                return;
            }
            JsonElement root = GSON.fromJson(
                    new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8), JsonElement.class);
            if (root == null || !root.isJsonObject()) {
                return;
            }
            JsonObject obj = root.getAsJsonObject();
            JsonElement modesEl = obj.get("modes");
            if (modesEl != null && modesEl.isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : modesEl.getAsJsonObject().entrySet()) {
                    if (e.getValue().isJsonPrimitive()) {
                        modes.put(e.getKey(), e.getValue().getAsBoolean());
                    }
                }
            }
            JsonElement currentEl = obj.get("current");
            if (currentEl != null && currentEl.isJsonPrimitive()) {
                current = currentEl.getAsString();
            }
        } catch (Exception ex) {
            LOGGER.warn("读取显示模式记忆失败（按默认值继续）: {}", FILE, ex);
        }
    }

    private static void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("current", current);
            JsonObject m = new JsonObject();
            for (Map.Entry<String, Boolean> e : modes.entrySet()) {
                m.addProperty(e.getKey(), e.getValue());
            }
            obj.add("modes", m);
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(obj), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LOGGER.warn("保存显示模式记忆失败（本次会话内仍然生效）: {}", FILE, ex);
        }
    }
}
