package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.AbilityItems;
import cn.blockforge.generated.origoldsar.GeneratedMod;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * 迅影符文在客户端对「使用物品减速」的补偿。
 *
 * <p>原版在 {@link LocalPlayer#aiStep()} 里，当玩家正在使用物品（举盾 / 拉弓 / 吃喝等）且
 * 不在骑乘时，会把移动输入的前进 / 左右分量乘以 0.2，造成明显减速。这部分完全是客户端
 * 逻辑（服务端只按客户端上报的输入计算移动），因此不必用 Mixin，只要在
 * {@link MovementInputUpdateEvent}（恰好在该次 ×0.2 之前触发）里把输入放大 5 倍，
 * 让 ×0.2 之后的结果等价于不减速即可。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SwiftnessRuneClient {
    private SwiftnessRuneClient() {
    }

    /** 原版在使用物品时对输入施加的减速系数（见 LocalPlayer.aiStep 的 *= 0.2F）。 */
    private static final float ITEM_USE_SLOWDOWN = 0.2F;

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof LocalPlayer) || player.isSpectator()) {
            return;
        }
        // 与原版减速条件一致：正在使用物品且未骑乘
        if (!player.isUsingItem() || player.isPassenger()) {
            return;
        }
        if (!AbilityItems.hasRuneInInventory(player, GeneratedMod.SWIFTNESS_RUNE.get())) {
            return;
        }
        Input input = event.getInput();
        float multiplier = 1.0F / ITEM_USE_SLOWDOWN;
        input.leftImpulse *= multiplier;
        input.forwardImpulse *= multiplier;
    }
}
