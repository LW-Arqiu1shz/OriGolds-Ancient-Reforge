package cn.blockforge.generated.origoldsar;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 本模组的“玩家信息栏”输出：把提示接入父模组《金源工艺》（origolds）新加入的公共
 * 玩家信息栏接口 {@code cn.arqiuowo.origolds.api.TextBroadcastService}。
 *
 * <p>统一走该接口后，多个模组的信息就能在同一栏里共存：父模组负责把“持续显示”的信息
 * 用 {@code " | "} 拼接，并在有持续信息存在时抑制本模组这类一次性的“滞留信息”。
 * 本模组的提示都是一次性的，因此统一用 {@code showRetained}，并在一小段时间后
 * {@code stop}，保持与原版 action bar 相同的“显示几秒后自行消失”手感。
 *
 * <p>父模组没有该接口（旧版本 / 未安装）时回退为原来的 {@link ServerPlayer#displayClientMessage}，
 * 行为与从前完全一致，不抛异常也不刷日志。用反射而非编译期引用，是为了让本模组在
 * 父模组加入该接口前后都能正常编译与运行。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PlayerInfoBar {
    /** 本模组在玩家信息栏里的唯一 ownerId：同 ownerId 的新信息直接替换旧信息。 */
    private static final String OWNER_ID = "origolds_ar:player_info";
    /** 提示在信息栏里停留的 tick 数（父模组每 tick 重发一次，客户端收到后还会再显示约 3 秒）。 */
    private static final int LINGER_TICKS = 5;

    private static final String SERVICE_CLASS = "cn.arqiuowo.origolds.api.TextBroadcastService";
    private static final String CHANNEL_CLASS = "cn.arqiuowo.origolds.api.TextBroadcastChannel";

    /** 是否已经尝试解析过父模组接口（无论成功失败都只尝试一次）。 */
    private static boolean resolved;
    /** 父模组的 {@code showRetained} / {@code stop} 方法与 {@code PLAYER_INFO} 枚举常量。 */
    private static Method showRetained;
    private static Method stop;
    private static Object playerInfoChannel;

    /** 玩家 UUID -> 该玩家当前提示应停止重发的服务端 tick。 */
    private static final Map<UUID, Integer> PENDING_STOP = new HashMap<>();

    private PlayerInfoBar() {}

    /** 在玩家的信息栏里显示一条一次性提示（服务端调用）。 */
    public static void show(ServerPlayer player, Component message) {
        if (player == null || message == null) {
            return;
        }
        resolve();
        MinecraftServer server = player.getServer();
        if (showRetained != null && playerInfoChannel != null && server != null) {
            try {
                showRetained.invoke(null, player, playerInfoChannel, OWNER_ID, message);
                PENDING_STOP.put(player.getUUID(), server.getTickCount() + LINGER_TICKS);
                return;
            } catch (Throwable ignored) {
                // 接口签名变更等：退回原版 action bar，别让提示丢失。
            }
        }
        player.displayClientMessage(message, true);
    }

    /** 到点后停掉本模组自己的滞留信息，让信息栏回到“无持续信息”的状态。 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING_STOP.isEmpty()) {
            return;
        }
        int now = event.getServer().getTickCount();
        Iterator<Map.Entry<UUID, Integer>> iterator = PENDING_STOP.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            if (now < entry.getValue()) {
                continue;
            }
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null && stop != null && playerInfoChannel != null) {
                try {
                    stop.invoke(null, player, playerInfoChannel, OWNER_ID);
                } catch (Throwable ignored) {
                    // 忽略：下一条提示仍会正常覆盖。
                }
            }
            iterator.remove();
        }
    }

    /** 解析父模组接口；只尝试一次，失败后永远按“接口不存在”处理。 */
    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> service = load(SERVICE_CLASS);
            Class<?> channel = load(CHANNEL_CLASS);
            Object info = null;
            for (Object constant : channel.getEnumConstants()) {
                if (constant instanceof Enum<?> candidate && "PLAYER_INFO".equals(candidate.name())) {
                    info = constant;
                    break;
                }
            }
            if (info == null) {
                return;
            }
            playerInfoChannel = info;
            showRetained = service.getMethod("showRetained", Player.class, channel, String.class, Component.class);
            stop = service.getMethod("stop", Player.class, channel, String.class);
        } catch (Throwable ignored) {
            showRetained = null;
            stop = null;
            playerInfoChannel = null;
        }
    }

    /** 先按本模组类加载器查找，再退回线程上下文类加载器（不同加载器下 mod 之间互不可见的情况都覆盖到）。 */
    private static Class<?> load(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name, false, PlayerInfoBar.class.getClassLoader());
        } catch (ClassNotFoundException first) {
            ClassLoader context = Thread.currentThread().getContextClassLoader();
            if (context == null) {
                throw first;
            }
            return Class.forName(name, false, context);
        }
    }
}
