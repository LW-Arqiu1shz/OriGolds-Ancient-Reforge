package cn.blockforge.generated.origoldsar;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 自循环附魔之瓶：一只可以反复投掷、不会被消耗的附魔之瓶。
 *
 * <ul>
 *   <li><b>右键</b>：消耗投掷者 10 点经验值，掷出一只命中时固定爆出 1 颗 10 经验经验球的瓶子；</li>
 *   <li><b>Ctrl + 右键</b>：消耗投掷者全部经验值（单次最多 {@value #MAX_ORB_XP} 点，
 *       即一颗经验球能存储的经验上限），掷出一只命中时把这笔经验一次性爆成 1 颗经验球的瓶子。</li>
 * </ul>
 *
 * <p>瓶子本身不消耗，经验 10 进 10 出、全进全出，故名“自循环”。Ctrl 是原版不会同步给服务端的
 * 修饰键，因此由客户端探测后经 {@code SelfBottlePayload} 上报（见
 * {@code client.SelfBottleClient} 与 {@code SelfBottleNetwork}）。
 */
public final class SelfCirculatingBottleItem extends Item {
    /** 普通投掷一次消耗的经验值。 */
    public static final int NORMAL_COST = 10;
    /** 一颗经验球能存储的经验上限，也是 Ctrl 投掷单次能带走的经验上限。 */
    public static final int MAX_ORB_XP = 32767;

    public SelfCirculatingBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.totalExperience < NORMAL_COST) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            player.giveExperiencePoints(-NORMAL_COST);
            throwBottle(level, player, NORMAL_COST);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_BOTTLE_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * 服务端处理 Ctrl + 右键：把玩家当前全部经验（最多 {@value #MAX_ORB_XP} 点）一次性
     * 消耗并掷出一只携带该经验量的瓶子。经验为 0 时什么都不做。
     */
    public static void throwAllExperience(Level level, Player player) {
        int amount = Math.min(player.totalExperience, MAX_ORB_XP);
        if (amount <= 0) {
            return;
        }
        player.giveExperiencePoints(-amount);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_BOTTLE_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        throwBottle(level, player, amount);
    }

    /** 服务端：以玩家视角掷出一只携带指定经验量的自循环附魔之瓶。 */
    private static void throwBottle(Level level, Player player, int xp) {
        SelfCirculatingBottleEntity bottle = new SelfCirculatingBottleEntity(level, player);
        bottle.setXpValue(xp);
        bottle.setItem(new ItemStack(GeneratedMod.SELF_CIRCULATING_BOTTLE.get()));
        bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.7F, 1.0F);
        level.addFreshEntity(bottle);
    }
}
