package cn.blockforge.generated.origoldsar;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 永恒药水：封装了一瓶原版药水（饮用/喷溅/滞留）的“永不被消耗”版本。
 * 底层药水的物品类型与效果内容保存在 {@code origolds_ar:eternal_base_potion} 组件里，
 * 同时镜像了 {@code minecraft:potion_contents} 组件，使客户端名称、提示与饮用逻辑复用原版行为。
 * 使用（喝完/掷出）后进入冷却：滞留型为 20 秒，饮用型与喷溅型为 10 秒。
 * 冷却按“每瓶”独立计算，存在该瓶自身的 {@code origolds_ar:eternal_cooldown}
 * 组件里（冷却结束时间戳，Unix 毫秒），互不影响。
 */
public final class EternalPotionItem extends CooldownDurabilityItem {
    public static final int DRINK_DURATION_TICKS = 20;
    /** 饮用型 / 喷溅型药水的冷却时长：10 秒。 */
    public static final long COOLDOWN_MILLIS = 10_000L;
    /** 滞留型药水的冷却时长：20 秒。 */
    public static final long LINGERING_COOLDOWN_MILLIS = 20_000L;

    private final Item bottleItem;

    public EternalPotionItem(Properties properties, Item bottleItem) {
        super(properties);
        this.bottleItem = bottleItem;
    }

    /** 用任意药水堆栈创建一个对应的永恒药水（饮用/喷溅/滞留，随材料药水类型选用对应物品与贴图）。 */
    public static ItemStack create(ItemStack potion) {
        Item base = potion.getItem();
        Item item;
        if (base == Items.SPLASH_POTION) {
            item = GeneratedMod.ETERNAL_SPLASH_POTION.get();
        } else if (base == Items.LINGERING_POTION) {
            item = GeneratedMod.ETERNAL_LINGERING_POTION.get();
        } else {
            item = GeneratedMod.ETERNAL_POTION.get();
        }
        ItemStack result = new ItemStack(item);
        applyBase(result, potion.copyWithCount(1));
        return result;
    }

    /** 是否为永恒药水系列的任意一类（饮用/喷溅/滞留）。 */
    public static boolean isEternalPotion(ItemStack stack) {
        return stack.is(GeneratedMod.ETERNAL_POTION.get())
                || stack.is(GeneratedMod.ETERNAL_SPLASH_POTION.get())
                || stack.is(GeneratedMod.ETERNAL_LINGERING_POTION.get());
    }

    private static void applyBase(ItemStack eternal, ItemStack base) {
        eternal.set(GeneratedMod.ETERNAL_BASE_POTION.get(), base);
        PotionContents contents = base.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            eternal.set(DataComponents.POTION_CONTENTS, contents);
        } else {
            eternal.remove(DataComponents.POTION_CONTENTS);
        }
    }

    /** 是否为可作为永恒药水材料的药水（饮用/喷溅/滞留，包括水瓶、粗制的药水等无效果药水）。 */
    public static boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    /** 永恒药水内部封装的原版药水堆栈（可能为 EMPTY）。 */
    public static ItemStack baseOf(ItemStack eternal) {
        ItemStack base = eternal.get(GeneratedMod.ETERNAL_BASE_POTION.get());
        return base == null ? ItemStack.EMPTY : base;
    }

    /** 该瓶自己的冷却结束时间戳（Unix 毫秒），未在冷却中时为 0。 */
    public static long cooldownEndOf(ItemStack eternal) {
        Long end = eternal.get(GeneratedMod.ETERNAL_COOLDOWN.get());
        return end == null ? 0L : end;
    }

    /** 该瓶剩余冷却毫秒数（每瓶独立计算）。 */
    public static long remainingCooldownMillis(ItemStack eternal) {
        return Math.max(0L, cooldownEndOf(eternal) - System.currentTimeMillis());
    }

    /** 该瓶当前是否在冷却中。 */
    public static boolean isOnCooldown(ItemStack eternal) {
        return remainingCooldownMillis(eternal) > 0;
    }

    /** 该瓶永恒药水对应的冷却时长：滞留型 20 秒，饮用型 / 喷溅型 10 秒。 */
    public static long cooldownMillisFor(ItemStack eternal) {
        return eternal.is(GeneratedMod.ETERNAL_LINGERING_POTION.get())
                ? LINGERING_COOLDOWN_MILLIS
                : COOLDOWN_MILLIS;
    }

    /** 开始该瓶自己的冷却（只影响这一瓶）；滞留型 20 秒，其余 10 秒。 */
    public static void startCooldown(ItemStack eternal) {
        eternal.set(GeneratedMod.ETERNAL_COOLDOWN.get(), System.currentTimeMillis() + cooldownMillisFor(eternal));
    }

    @Override
    protected long cooldownEndMillis(ItemStack stack) {
        return cooldownEndOf(stack);
    }

    @Override
    protected long cooldownTimeoutMillis(ItemStack stack) {
        return cooldownMillisFor(stack);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack base = new ItemStack(this.bottleItem);
        base.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        return create(base);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack base = baseOf(stack);
        if (base.isEmpty()) {
            return Component.translatable(this.getDescriptionId(stack));
        }
        return Component.translatable("text.origolds_ar.eternal_prefix").append(base.getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ItemStack base = baseOf(stack);
        if (!base.isEmpty()) {
            base.getItem().appendHoverText(base, context, tooltipComponents, tooltipFlag);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack base = baseOf(stack);
        if (base.isEmpty() || !isPotion(base)) {
            return InteractionResultHolder.fail(stack);
        }
        if (isOnCooldown(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        Item baseItem = base.getItem();
        if (baseItem == Items.SPLASH_POTION || baseItem == Items.LINGERING_POTION) {
            if (!level.isClientSide) {
                ThrownPotion thrown = new ThrownPotion(level, player);
                thrown.setItem(base.copyWithCount(1));
                thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
                level.addFreshEntity(thrown);
                startCooldown(stack);
            }
            player.playSound(baseItem == Items.LINGERING_POTION ? SoundEvents.LINGERING_POTION_THROW : SoundEvents.SPLASH_POTION_THROW,
                    0.5F, 0.4F / (level.getRandom().nextFloat() * 0.8F + 0.8F));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (baseItem instanceof PotionItem) {
            // 饮用：委托给原版 PotionItem（开始饮用动画；物品不会被消耗，
            // 结束时的效果应用与冷却在本类的 finishUsingItem 中处理）。
            return baseItem.use(level, player, hand);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        ItemStack base = baseOf(stack);
        if (base.getItem() instanceof PotionItem potionItem && !level.isClientSide) {
            potionItem.finishUsingItem(base.copyWithCount(1), level, living);
        }
        if (living instanceof Player player && !level.isClientSide) {
            startCooldown(stack);
        }
        return stack;
    }
}
