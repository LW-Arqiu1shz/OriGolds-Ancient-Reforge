package cn.blockforge.generated.origoldsar.mixin;

import cn.blockforge.generated.origoldsar.MoonwalkNetwork;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 月球重力：把玩家下落时的垂直空气阻力从原版的 0.98F 调到让“最大终端下落速度”为原来的 40%。
 *
 * <p>（从 1.21.1 移植。26.x 移植时漏掉了这个 mixin，月球重力下终端速度只剩原版的 25%，
 * 与 1.21.1 的 40% 不一致。）
 *
 * <p>26.x 的空中移动拆到了 {@code LivingEntity.travelInAir}，垂直阻力系数是该方法里唯一的
 * {@code 0.98F} 常量（26.1.2 直接相乘；26.3 先经 {@code computeModifiedFriction(0.98F, 空气阻力修正属性)}，
 * 修正属性默认 1.0 时原样返回）。把系数改成 {@code LUNAR_TERMINAL / (gravity + LUNAR_TERMINAL)}：
 * 对任意正重力 g，终端速度都恒等于 {@code LUNAR_TERMINAL}（原版终端的 40%）。
 *
 * <p>仅对“月球重力已激活”且“双倍重力未激活”的玩家生效；重力为 0 或负（同时开了反转重力）时不干预。
 */
@Mixin(LivingEntity.class)
public abstract class LunarGravityMixin {

    @ModifyConstant(method = "travelInAir(Lnet/minecraft/world/phys/Vec3;)V", constant = @Constant(floatValue = 0.98F))
    private float origoldsAr$lunarAirDrag(float original) {
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
        return (float) (MoonwalkNetwork.LUNAR_TERMINAL_FALL / (gravity + MoonwalkNetwork.LUNAR_TERMINAL_FALL));
    }
}
