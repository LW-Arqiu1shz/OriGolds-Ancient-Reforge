package cn.blockforge.generated.origoldsar.mixin;

import cn.blockforge.generated.origoldsar.MoonwalkNetwork;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 月球重力：把玩家下落时的垂直空气阻力从原板的 0.98F 调到让“最大终端下落速度”为原来的 40%。
 *
 * <p>原版 {@code LivingEntity.travel()} 在非水上/岩浆/滑翔分支里对垂直速度乘一个固定系数
 * {@code 0.98F}，由此得到的终端下落速度 ≈ {@code 0.98 * 0.08 / (1 - 0.98) = 3.92} 格/秒。
 * 月球重力把重力属性缩放到 25%（0.02），若不动空气阻力，终端速度会随之降到原来的 25%。
 * 为了让终端速度恰好落在“原来的 40%”，这里把系数改成
 * {@code LUNAR_TERMINAL / (gravity + LUNAR_TERMINAL)}：经验证，对任意正重力 g，该系数
 * 都会让终端速度恒等于 {@code LUNAR_TERMINAL}（即原版终端的 40%），不受重力缩放影响。
 *
 * <p>仅在“月球重力已激活（存在月球重力修饰符）”且“双倍重力未激活”的玩家身上生效；其它实体与普通玩家一律返回原值。
 * 双倍重力（空中按住潜行）会把 gravity 临时抬回 0.16，此时不应再套用月球重力的终端速度上限，
 * 因此这里直接交还原板系数。重力为 0 或负数（例如反转重力也同时激活，重力变为 -0.02）时不干预，
 * 交还原板系数。
 */
@Mixin(LivingEntity.class)
public abstract class LunarGravityMixin {

    @ModifyConstant(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", constant = @Constant(doubleValue = 0.9800000190734863D))
    private double origoldsAr$lunarAirDrag(double original) {
        // mixin 里的 this 类型是 LunarGravityMixin（运行时是 LivingEntity），
        // 需要先 cast 成 Object 才能用 instanceof 判断玩家，否则编译器认为类型不兼容。
        if (!((Object) this instanceof Player player)) {
            return original;
        }
        if (!MoonwalkNetwork.hasLunarGravity(player) || MoonwalkNetwork.isDoubleGravityActive(player)) {
            return original;
        }
        double gravity = player.getAttributeValue(Attributes.GRAVITY);
        if (gravity <= 0.0D) {
            return original;
        }
        return MoonwalkNetwork.LUNAR_TERMINAL_FALL / (gravity + MoonwalkNetwork.LUNAR_TERMINAL_FALL);
    }
}
