package cn.blockforge.generated.origoldsar;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 钩爪弩炮（Grapnel Ballista）：外形与使用方式都是一把弩，
 * 在弩的基础上额外把“主动射出的那根主箭”标记出来，供钩爪绑定系统追踪。
 *
 * <p>继承 {@link CrossbowItem} 即自动获得弩的全部行为：蓄力时长、装填音效、
 * 附魔台可附魔、只吃箭、以及多重射击/穿透等弩能接受的附魔效果；
 * 每根射出的箭上都带有本物品作为 {@code firedFromWeapon}，据此识别。
 */
public class GrapnelBallista extends CrossbowItem {
    /** 箭矢持久数据里标记“本根是主动射出的主箭（多重射击的附加箭不算）”的键。 */
    public static final String MAIN_ARROW_TAG = "origolds_ar_grapnel_main";

    public GrapnelBallista(Properties properties) {
        super(properties);
    }


    /**
     * 每根创建出来的箭都先打上“主箭候选”标记；真正绑定发生在
     * {@code EntityJoinLevelEvent} 里——同一 tick 内最后一根加入世界时覆盖标记，
     * 而多重射击的附加箭是在主箭之后才创建的，因此最终留下标记的是主箭。
     */
    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit) {
        Projectile fired = super.createProjectile(level, shooter, weapon, projectile, isCrit);
        fired.getPersistentData().putBoolean(MAIN_ARROW_TAG, true);
        return fired;
    }

    public static boolean isMarkedMainArrow(net.minecraft.world.entity.Entity entity) {
        return entity.getPersistentData().getBooleanOr(MAIN_ARROW_TAG, false);
    }

    public static void clearMainArrowMark(net.minecraft.world.entity.Entity entity) {
        entity.getPersistentData().remove(MAIN_ARROW_TAG);
    }
}
