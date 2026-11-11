package cn.blockforge.generated.origoldsar.mixin;

import cn.blockforge.generated.origoldsar.MoonwalkNetwork;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 月亮行者勋章（以及继承其能力的暗影勋章、永夜擢升之证）：持有者免疫摔落伤害。
 *
 * <p>1.21.1 中落地伤害的实际触发链是 {@code Entity.move()} → {@code checkFallDamage()} →
 * {@code Block.fallOn()} → {@code Player.causeFallDamage()}。玩家落地时由方块调用
 * {@code Player.causeFallDamage(float, float, DamageSource)}，该方法（经 {@code LivingEntity}
 * 父类实现）最终通过 {@code hurt()} 施加伤害并播放落地音效。
 *
 * <p>这里在 {@code Player.causeFallDamage()} 的 HEAD 处拦截：持勋章时直接把返回值改为
 * {@code false}（表示“未造成伤害”）并跳过后续所有逻辑。这样既不会扣血、也不会播放落地音效、
 * 更不会计入玩家“掉落”统计。摔落距离随后仍会被 {@code Entity.checkFallDamage()} 里的
 * {@code resetFallDistance()} 清零，不会累积到下一次落地。
 *
 * <p>判定基于 {@link MoonwalkNetwork#hasMoonwalker(Player)}，与其它勋章能力一致：
 * 持本体、暗影勋章、永夜擢升之证中的任意一件都视为持有。
 */
@Mixin(Player.class)
public abstract class FallImmunityMixin {

    @Inject(method = "causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z",
            at = @At("HEAD"), cancellable = true)
    private void origoldsAr$fallImmunity(double distance, float damageMultiplier,
                                         DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (MoonwalkNetwork.hasMoonwalker((Player) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
