package cn.blockforge.generated.origoldsar;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 复苏图腾（Totem of Resurrection）：一颗“永不被消耗”的不死图腾。
 * 只要它位于玩家的背包中（任意一格，无需手持），玩家即将死亡时就会触发复活
 * （与原版不死图腾相同的效果：回满 1 点生命并施加再生/伤害吸收/抗火）。
 *
 * <p>冷却按“每根”独立计算，存在该根自身的 {@code origolds_ar:totem_cooldown}
 * 组件里（冷却结束时间戳，Unix 毫秒），因此每根图腾的冷却进度互不影响；
 * 不过当玩家触发图腾效果时，会让该玩家身上的所有复苏图腾一起进入 180 秒冷却
 * （见 {@link TotemResurrectionEvents}）。
 *
 * <p>继承 {@link CooldownDurabilityItem}，把冷却进度渲染成物品下方的那条耐久条：
 * 冷却开始时耐久为 0，随冷却进度恢复到 100（满）。该耐久值没有任何实际意义。
 */
public final class TotemOfResurrectionItem extends CooldownDurabilityItem {
    /** 所有复苏图腾统一的冷却时长：180 秒。 */
    public static final long COOLDOWN_MILLIS = 180_000L;

    public TotemOfResurrectionItem(Properties properties) {
        super(properties);
    }

    /** 该根图腾自己的冷却结束时间戳（Unix 毫秒），未在冷却中时为 0。 */
    public static long cooldownEndOf(ItemStack totem) {
        Long end = totem.get(GeneratedMod.TOTEM_COOLDOWN.get());
        return end == null ? 0L : end;
    }

    /** 该根图腾当前是否在冷却中。 */
    public static boolean isOnCooldown(ItemStack totem) {
        return cooldownEndOf(totem) > System.currentTimeMillis();
    }

    /** 开始该根图腾自己的 180 秒冷却（只影响这一根）。 */
    public static void startCooldown(ItemStack totem) {
        totem.set(GeneratedMod.TOTEM_COOLDOWN.get(), System.currentTimeMillis() + COOLDOWN_MILLIS);
    }

    /** 是否为复苏图腾。 */
    public static boolean isTotem(ItemStack stack) {
        return stack.is(GeneratedMod.TOTEM_OF_RESURRECTION.get());
    }

    @Override
    protected long cooldownEndMillis(ItemStack stack) {
        return cooldownEndOf(stack);
    }

    @Override
    protected long cooldownTimeoutMillis(ItemStack stack) {
        return COOLDOWN_MILLIS;
    }
}
