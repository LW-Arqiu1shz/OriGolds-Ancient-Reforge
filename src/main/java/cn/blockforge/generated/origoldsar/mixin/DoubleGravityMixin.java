package cn.blockforge.generated.origoldsar.mixin;

import cn.blockforge.generated.origoldsar.MoonwalkNetwork;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 双倍重力：玩家在空中按住潜行时，把 {@code Entity.getGravity()} 的返回值临时强制为
 * “常态重力的两倍”，实现“空中按住潜行→以两倍常态重力下坠”。
 *
 * <p>之所以直接改 {@code getGravity()} 的返回值而不是像月球重力那样走属性修饰符，
 * 是因为属性修饰符的叠加顺序（{@code ADD_MULTIPLIED_TOTAL} 最后被乘上）无法用一个静态修饰符
 * 同时兼顾“月球重力未开时也等于两倍常态、开了月球重力时也等于两倍常态（0.16）”两种情况；
 * 而“常态的两倍”应始终等于 2 × 属性基础值，故在 RETURN 处按需替换返回值最干净。
 *
 * <p>该判定在客户端与服务端都基于各自的玩家状态（{@code onGround()} 与 {@code isShiftKeyDown()}）
 * 独立计算，两边的 {@code getGravity()} 因此得到一致值，客户端移动预测与服务端不会来回弹跳。
 * 松开潜行或落到地面，条件不成立，自动恢复为属性当前值（未开月球重力为常态重力，开了则为月球重力）。
 */
@Mixin(Entity.class)
public abstract class DoubleGravityMixin {

    @Inject(method = "getGravity()D", at = @At("RETURN"), cancellable = true)
    private void origoldsAr$doubleGravity(CallbackInfoReturnable<Double> cir) {
        // mixin 里的 this 类型是 DoubleGravityMixin（运行时是 Entity），
        // 需要先 cast 成 Object 才能用 instanceof 判断玩家，否则编译器认为类型不兼容。
        if ((Object) this instanceof Player player && MoonwalkNetwork.isDoubleGravityActive(player)) {
            cir.setReturnValue(MoonwalkNetwork.doubleGravityValue(player));
        }
    }
}
