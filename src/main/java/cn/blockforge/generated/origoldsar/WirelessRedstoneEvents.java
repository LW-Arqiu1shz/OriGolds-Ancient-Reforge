package cn.blockforge.generated.origoldsar;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerPlayer;

/** 无线红石的服务端生命周期挂钩：每 tick 同步虚拟电源、玩家进服同步线路、退档清理。 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class WirelessRedstoneEvents {
    private WirelessRedstoneEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        WirelessRedstoneManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WirelessRedstoneManager.syncTo(player);
            WirelessRedstoneManager.syncSelection(player);
        }
    }

    /** 玩家退出世界：清除他记录的撤销/重做数据（线路历史 + 名下所有遥控开关的历史）。 */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        WirelessHistory.clearPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            WirelessRedstoneManager.onLevelUnload(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WirelessRedstoneManager.clearRuntime();
    }
}
