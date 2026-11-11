package cn.blockforge.generated.origoldsar;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 玩家信息栏（1.2.0 新功能）：把原本刷在 action bar 上的操作提示优先投递到
 * 基模《金源工艺》提供的玩家信息栏（TextBroadcastService 的 PLAYER_INFO 频道），
 * 停留 {@value #LINGER_TICKS} tick 后自动清除；基模不在或接口缺失时
 * 回退为原版 action bar，行为与旧版完全一致。
 *
 * <p>基模是软依赖且接口只在服务端可见，所以这里全程用反射探测 + 优雅降级，
 * 不产生编译期类依赖。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class PlayerInfoBar {
    /** 同一玩家同一频道内的所有权 id：stop 时按它精确清除自己的消息。 */
    private static final String OWNER_ID = "origolds_ar:player_info";
    /** 消息在信息栏上的停留时长（tick），到点后由服务端 tick 钩子统一 stop。 */
    private static final int LINGER_TICKS = 5;
    private static final String SERVICE_CLASS = "cn.arqiuowo.origolds.api.TextBroadcastService";
    private static final String CHANNEL_CLASS = "cn.arqiuowo.origolds.api.TextBroadcastChannel";

    private static boolean resolved;
    private static Method showRetained;
    private static Method stop;
    /** 基模 TextBroadcastChannel.PLAYER_INFO 枚举常量；探测失败时为 null。 */
    private static Object playerInfoChannel;
    /**  playerId -> 该玩家消息的到期 tick。 */
    private static final Map<UUID, Integer> PENDING_STOP = new HashMap<>();

    private PlayerInfoBar() {}

    /** 向玩家展示一条信息栏提示（优先信息栏频道，回退 action bar）。 */
    public static void show(ServerPlayer player, Component message) {
        if (player == null || message == null) {
            return;
        }
        resolve();
        MinecraftServer server = player.level().getServer();
        if (showRetained != null && playerInfoChannel != null && server != null) {
            try {
                showRetained.invoke(null, player, playerInfoChannel, OWNER_ID, message);
                synchronized (PENDING_STOP) {
                    PENDING_STOP.put(player.getUUID(), server.getTickCount() + LINGER_TICKS);
                }
                return;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // 基模接口异常：回退 action bar。
            }
        }
        player.sendOverlayMessage(message);
    }

    /** 停留期满后清除信息栏消息（服务端 tick 收尾阶段）。 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        synchronized (PENDING_STOP) {
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
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // 清除失败不影响游戏。
                    }
                }
                iterator.remove();
            }
        }
    }

    /** 懒加载探测基模的信息栏接口；只探测一次，失败即永久降级。 */
    private static synchronized void resolve() {
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
            showRetained = service.getMethod("showRetained",
                    Player.class, channel, String.class, Component.class);
            stop = service.getMethod("stop", Player.class, channel, String.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            showRetained = null;
            stop = null;
            playerInfoChannel = null;
        }
    }

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
