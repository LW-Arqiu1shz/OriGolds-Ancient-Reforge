package cn.blockforge.generated.origoldsar;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 兜底：若玩家处于反转状态（GRAVITY 属性被改为负）、却已不再持有任何能提供该能力的
 * 物品（月亮行者勋章 / 暗影勋章 / 永夜擢升之证），则自动恢复重力，避免卡在倒立状态无法关闭。
 * 只在逻辑服务端执行（属性以服务端为准，客户端改动会被覆盖）。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class MoonwalkResetEvents {
    private MoonwalkResetEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        MoonwalkNetwork.clearIfNotHeld(player);
    }
}
