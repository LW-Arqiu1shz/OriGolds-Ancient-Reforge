package cn.blockforge.generated.origoldsar;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 自循环附魔之瓶的飞行实体（1.2.0 新功能）：外观与弹道模仿经验瓶
 * （0.07 重力），落地后在命中点爆出与装入量等值的经验球——
 * 也就是把玩家自己的经验“自循环”搬运回来，可越障、可投掷、不丢失。
 *
 * <p>装瓶经验量随实体存档（{@code XpValue}，上限一只瓶子的 32767）。
 */
public final class SelfCirculatingBottleEntity extends ThrowableItemProjectile {
    private static final String TAG_XP = "XpValue";
    private int xpValue = SelfCirculatingBottleItem.NORMAL_COST;

    public SelfCirculatingBottleEntity(EntityType<? extends SelfCirculatingBottleEntity> type, Level level) {
        super(type, level);
    }

    public SelfCirculatingBottleEntity(Level level, LivingEntity shooter, ItemStack contents) {
        super(GeneratedMod.SELF_CIRCULATING_BOTTLE_ENTITY.get(), shooter, level, contents);
    }

    public void setXpValue(int xpValue) {
        this.xpValue = Math.max(0, Math.min(Short.MAX_VALUE, xpValue));
    }

    public int getXpValue() {
        return this.xpValue;
    }

    @Override
    protected Item getDefaultItem() {
        return GeneratedMod.SELF_CIRCULATING_BOTTLE.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.07D;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        // 2002 = 喷溅药水粒子的原版事件，颜色取水瓶色。
        server.levelEvent(2002, this.blockPosition(), new PotionContents(Potions.WATER).getColor());
        Vec3 pos = result.getLocation();
        server.addFreshEntity(new ExperienceOrb(server, pos.x, pos.y, pos.z, this.xpValue));
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt(TAG_XP, this.xpValue);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setXpValue(input.getIntOr(TAG_XP, SelfCirculatingBottleItem.NORMAL_COST));
    }
}
