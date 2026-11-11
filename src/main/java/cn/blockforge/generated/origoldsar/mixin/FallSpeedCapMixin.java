package cn.blockforge.generated.origoldsar.mixin;

import cn.blockforge.generated.origoldsar.MoonwalkNetwork;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 超重力下落速度上限：只约束“超重力结束那一刻”的下落速度，超重力期间让速度按超重力
 * 自身的上限走，而不是被普通 / 月球模式的上限提前压住。
 *
 * <p>在 {@code LivingEntity.travel()} 的出口处（{@code TAIL}）判定：
 * <ul>
 *   <li><b>超重力激活</b>（空中按住潜行、双倍重力）：按超重力自己的终端速度
 *       {@link MoonwalkNetwork#SUPERGRAVITY_TERMINAL_FALL}（约原版两倍，7.84 格/tick）
 *       当上限——这是“按超重力的最大速度来”，不会把速度压到普通 / 月球的上限。</li>
 *   <li><b>超重力未激活</b>：若当前下落速度仍高于当前模式的上限
 *       {@link MoonwalkNetwork#fallSpeedCap(Player)}（普通=原版终端 3.92，月亮行者=原版 40%），
 *       则立刻重置回该上限——这正是“只有超重力结束的时候才卡上限”。</li>
 * </ul>
 *
 * <p>“超重力结束那一刻”通过“当前不处于超重力但速度超过模式上限”来捕捉：超重力生效期间
 * 速度按 7.84 的上限累积，松开潜行后第一拍速度仍高于 3.92/1.568，于是被压回当前模式的上限；
 * 而普通/月亮行者自由下落本就收敛到各自终端速度，不会超过、也就不会被误压。
 *
 * <p>滑翔（{@code isFallFlying()}，如鞘翅俯冲）属于正常的高速下落，不参与该上限，避免误伤。
 * 该判定基于玩家自身状态（{@code onGround()}、{@code isShiftKeyDown()}、是否持勋章），
 * 客户端与服务端各自独立计算，两边结果一致，不会导致移动预测与服务端来回弹跳。
 */
@Mixin(LivingEntity.class)
public abstract class FallSpeedCapMixin {

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void origoldsAr$capFallSpeed(Vec3 travelVector, CallbackInfo ci) {
        // mixin 里的 this 类型是 FallSpeedCapMixin（运行时是 LivingEntity），
        // 需要先 cast 成 Object 才能用 instanceof 判断玩家，否则编译器认为类型不兼容。
        if (!((Object) this instanceof Player player)) {
            return;
        }
        if (MoonwalkNetwork.isDoubleGravityActive(player)) {
            // 超重力激活：按超重力自身的上限，不按普通/月球的。
            clampFallSpeed(player, MoonwalkNetwork.SUPERGRAVITY_TERMINAL_FALL);
            return;
        }
        // 超重力未激活（含刚取消的那一刻）：只对勋章持有者做“速度超上限则重置”。
        if (!MoonwalkNetwork.hasMoonwalker(player) || player.isFallFlying()) {
            return;
        }
        clampFallSpeed(player, MoonwalkNetwork.fallSpeedCap(player));
    }

    private static void clampFallSpeed(Player player, double cap) {
        if (cap <= 0.0D) {
            return;
        }
        Vec3 delta = player.getDeltaMovement();
        if (delta.y < -cap) {
            player.setDeltaMovement(delta.x, -cap, delta.z);
        }
    }
}
