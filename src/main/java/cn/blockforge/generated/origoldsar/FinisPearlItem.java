package cn.blockforge.generated.origoldsar;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 终末之影珍珠（Finis Pearl）：一颗“永不被消耗”的末影珍珠。
 * 使用（右键掷出）后像原版末影珍珠一样把使用者瞬移到落点，但物品本身不会被消耗；
 * 使用后进入 10 秒冷却。冷却按“每颗”独立计算，
 * 存在该颗珍珠自身的 {@code origolds_ar:finis_cooldown} 组件里（冷却结束时间戳，Unix 毫秒），
 * 互不影响。
 */
public final class FinisPearlItem extends CooldownDurabilityItem {
    /** 所有珍珠统一的冷却时长：10 秒。 */
    public static final long COOLDOWN_MILLIS = 10_000L;

    public FinisPearlItem(Properties properties) {
        super(properties);
    }

    /** 该颗珍珠自己的冷却结束时间戳（Unix 毫秒），未在冷却中时为 0。 */
    public static long cooldownEndOf(ItemStack pearl) {
        Long end = pearl.get(GeneratedMod.FINIS_COOLDOWN.get());
        return end == null ? 0L : end;
    }

    /** 该颗珍珠当前是否在冷却中。 */
    public static boolean isOnCooldown(ItemStack pearl) {
        return cooldownEndOf(pearl) > System.currentTimeMillis();
    }

    /** 开始该颗珍珠自己的 10 秒冷却（只影响这一颗）。 */
    public static void startCooldown(ItemStack pearl) {
        pearl.set(GeneratedMod.FINIS_COOLDOWN.get(), System.currentTimeMillis() + COOLDOWN_MILLIS);
    }

    @Override
    protected long cooldownEndMillis(ItemStack stack) {
        return cooldownEndOf(stack);
    }

    @Override
    protected long cooldownTimeoutMillis(ItemStack stack) {
        return COOLDOWN_MILLIS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        player.playSound(SoundEvents.ENDER_PEARL_THROW,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.8F + 0.8F));
        if (!level.isClientSide()) {
            ThrownEnderpearl thrown = new ThrownEnderpearl(level, player, new ItemStack(this));
            thrown.setItem(new ItemStack(Items.ENDER_PEARL));
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(thrown);
            startCooldown(stack);
        }
        return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }
}
