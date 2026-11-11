package cn.blockforge.generated.origoldsar;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 具有“每件独立冷却”的道具基类：把冷却进度渲染成物品下方的那条耐久条。
 *
 * <p>规则：耐久条满值为 {@link #MAX_DURABILITY}（100）。使用（进入冷却）时耐久为 0，
 * 冷却进行中随进度自动恢复，冷却结束时耐久回到 100。
 * 以 10 秒冷却为例：每 1 秒恢复 10 点耐久；若某道具冷却为 15 秒，则每 1.5 秒恢复 10 点。
 * 恢复的速率是“按冷却时长线性”的，完全由 {@link #cooldownTimeoutMillis()} 决定，
 * 因此新道具只需实现该返回值即可复用本基类，无需改动恢复逻辑。
 *
 * <p>该耐久值没有任何实际意义：它只用作进度条。道具没有配置耐久消耗，破坏方块、
 * 攻击等操作不会减少它（本基类不写入 {@code minecraft:damage} / {@code minecraft:max_damage} 组件，
 * 而是直接覆写耐久条渲染，因此天然不会触发原版的耐久消耗逻辑）。
 */
public abstract class CooldownDurabilityItem extends Item {
    /** 耐久条满值。 */
    public static final int MAX_DURABILITY = 100;

    protected CooldownDurabilityItem(Properties properties) {
        super(properties);
    }

    /** 该件物品自己的冷却结束时间戳（Unix 毫秒），未在冷却中时返回 0。 */
    protected abstract long cooldownEndMillis(ItemStack stack);

    /** 该物品统一的冷却时长（毫秒）；按具体物品堆栈返回，便于不同类型/物品使用不同冷却。 */
    protected abstract long cooldownTimeoutMillis(ItemStack stack);

    /**
     * 当前耐久值（0……{@link #MAX_DURABILITY}），等于冷却恢复进度。
     * 不在冷却中时返回满值。
     */
    public int currentDurability(ItemStack stack) {
        long end = cooldownEndMillis(stack);
        if (end <= 0L) {
            return MAX_DURABILITY;
        }
        long timeout = cooldownTimeoutMillis(stack);
        if (timeout <= 0L) {
            return MAX_DURABILITY;
        }
        long remaining = end - System.currentTimeMillis();
        if (remaining <= 0L) {
            return MAX_DURABILITY;
        }
        double progress = 1.0 - (double) remaining / (double) timeout;
        return (int) Math.round(Math.min(1.0, progress) * MAX_DURABILITY);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return cooldownEndMillis(stack) > System.currentTimeMillis();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * currentDurability(stack) / MAX_DURABILITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // 冷却刚开始（耐久 0）为红色，随进度恢复逐步过渡到满值（耐久 100）的绿色。
        float hue = currentDurability(stack) / 3.0F / MAX_DURABILITY;
        return Mth.hsvToRgb(hue, 1.0F, 1.0F);
    }
}
