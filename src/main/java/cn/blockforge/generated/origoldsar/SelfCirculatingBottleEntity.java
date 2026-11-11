package cn.blockforge.generated.origoldsar;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 自循环附魔之瓶的投掷物：命中时<b>只爆出一颗经验球</b>，经验量由
 * {@link #setXpValue(int)} 指定（普通投掷为 10，Ctrl 投掷为玩家全部经验，最多 32767）。
 *
 * <p>原版 {@code ThrownExperienceBottle} 命中时会随机撒出 3~11 点经验、且可能被拆成多颗，
 * 与本模组“固定一颗、定量”的需求不符，所以这里另建实体并自行处理命中结算。
 * 外观直接复用原版附魔之瓶的投掷物渲染（{@code ThrownItemRenderer}）。
 */
public final class SelfCirculatingBottleEntity extends ThrowableItemProjectile {
    private static final String TAG_XP = "XpValue";

    /** 命中时要一次性爆出的经验球经验量（1~32767）。 */
    private int xpValue = SelfCirculatingBottleItem.NORMAL_COST;

    public SelfCirculatingBottleEntity(EntityType<? extends SelfCirculatingBottleEntity> type, Level level) {
        super(type, level);
    }

    public SelfCirculatingBottleEntity(Level level, LivingEntity shooter) {
        super(GeneratedMod.SELF_CIRCULATING_BOTTLE_ENTITY.get(), shooter, level);
    }

    public void setXpValue(int xpValue) {
        this.xpValue = Math.max(0, Math.min(SelfCirculatingBottleItem.MAX_ORB_XP, xpValue));
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
        // 与原版附魔之瓶一致的破碎粒子（颜色取水瓶色）。
        server.levelEvent(2002, this.blockPosition(), PotionContents.getColor(Potions.WATER));
        Vec3 pos = result.getLocation();
        // 直接构造经验球并加入世界：一次只产生一颗，经验量精确可控（上限 32767）。
        server.addFreshEntity(new ExperienceOrb(server, pos.x, pos.y, pos.z, this.xpValue));
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_XP, this.xpValue);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_XP)) {
            this.setXpValue(tag.getInt(TAG_XP));
        }
    }
}
