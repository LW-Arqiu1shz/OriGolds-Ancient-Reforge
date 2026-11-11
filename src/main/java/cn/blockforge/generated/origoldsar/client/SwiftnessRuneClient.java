package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.AbilityItems;
import cn.blockforge.generated.origoldsar.GeneratedMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * 迅影符文在客户端对「使用物品减速」的补偿。
 *
 * <p>原版在 {@code LocalPlayer.modifyInput} 里，当玩家正在使用物品（举盾 / 拉弓 / 吃喝等）且
 * 不在骑乘时，会把移动向量乘以 {@code itemUseSpeedMultiplier()}（默认 0.2），造成明显减速。
 * 这部分完全是客户端逻辑（服务端只按客户端上报的输入计算移动），因此不必用 Mixin。
 *
 * <p>26.1.2 的输入模型已改为记录式的 {@code net.minecraft.world.entity.player.Input} +
 * {@code ClientInput#moveVector}（protected，没有公开 setter），旧版的
 * {@code Input#leftImpulse/forwardImpulse} 字段不存在了。这里改为在
 * {@link MovementInputUpdateEvent}（恰好在 {@code modifyInput} 之前触发）里把玩家的
 * {@code ClientInput} 换成一个 {@link KeyboardInput} 子类，在
 * {@link KeyboardInput#getMoveVector()} 上放大 {@code 1/0.2 = 5} 倍；
 * {@code modifyInput} 随后又乘 0.2 并归一化，净效果等于不减速，与原实现一致。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class SwiftnessRuneClient {
    private SwiftnessRuneClient() {
    }

    /** 原版在使用物品时对输入施加的减速系数（见 UseEffects.DEFAULT 的 speedMultiplier）。 */
    private static final float ITEM_USE_SLOWDOWN = 0.2F;

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof LocalPlayer localPlayer) || player.isSpectator()) {
            return;
        }
        // 懒替换：登录/重生时原版会把 input 重置为 KeyboardInput，这里每次 tick 校验一次。
        if (localPlayer.input instanceof CompensatingInput) {
            return;
        }
        Options options = Minecraft.getInstance().options;
        if (options != null) {
            localPlayer.input = new CompensatingInput(localPlayer, options);
        }
    }

    /** 在“使用物品且持有迅影符文”时把移动向量放大 5 倍，抵消随后的 0.2 减速。 */
    private static final class CompensatingInput extends KeyboardInput {
        private final LocalPlayer player;

        private CompensatingInput(LocalPlayer player, Options options) {
            super(options);
            this.player = player;
        }

        @Override
        public Vec2 getMoveVector() {
            Vec2 base = super.getMoveVector();
            if (this.player.isUsingItem()
                    && !this.player.isPassenger()
                    && AbilityItems.hasRuneInInventory(this.player, GeneratedMod.SWIFTNESS_RUNE.get())) {
                return base.scale(1.0F / ITEM_USE_SLOWDOWN);
            }
            return base;
        }
    }
}
