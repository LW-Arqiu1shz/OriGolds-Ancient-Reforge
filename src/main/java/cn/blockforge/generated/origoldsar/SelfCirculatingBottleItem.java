package cn.blockforge.generated.origoldsar;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 自循环附魔之瓶（1.2.0 新功能）：把自己身上的经验装瓶掷出，落地后原样爆出，
 * 用来把经验"搬运"过地形或存到指定地点。
 *
 * <ul>
 *   <li>右键：固定消耗 10 点经验，掷出一瓶装 10 点经验的瓶子；经验不足 10 点时不响应。</li>
 *   <li>Ctrl + 右键（见 {@code client.SelfBottleClient}）：把当前经验一次性全部装瓶掷出，
 *       单瓶上限 32767 点（一只原版经验瓶的装载量），服务端复核持瓶状态。</li>
 * </ul>
 */
public final class SelfCirculatingBottleItem extends Item {
    /** 普通右键一次装瓶的经验点数。 */
    public static final int NORMAL_COST = 10;
    /** 单瓶可装的经验上限（与原版经验瓶一致）。 */
    public static final int MAX_ORB_XP = Short.MAX_VALUE;

    public SelfCirculatingBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.totalExperience < NORMAL_COST) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            player.giveExperiencePoints(-NORMAL_COST);
            throwBottle(level, player, NORMAL_COST);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_BOTTLE_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    /** 把玩家当前经验（截到单瓶上限）全部装瓶掷出，由服务端在收到 Ctrl+右键包后调用。 */
    public static void throwAllExperience(Level level, Player player) {
        int amount = Math.min(player.totalExperience, MAX_ORB_XP);
        if (amount <= 0) {
            return;
        }
        player.giveExperiencePoints(-amount);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_BOTTLE_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        throwBottle(level, player, amount);
    }

    private static void throwBottle(Level level, Player player, int xp) {
        SelfCirculatingBottleEntity bottle = new SelfCirculatingBottleEntity(
                level, player, new ItemStack(GeneratedMod.SELF_CIRCULATING_BOTTLE.get()));
        bottle.setXpValue(xp);
        // 出手点压低 20°，贴近原版经验瓶的投掷观感。
        bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.7F, 1.0F);
        level.addFreshEntity(bottle);
    }
}
