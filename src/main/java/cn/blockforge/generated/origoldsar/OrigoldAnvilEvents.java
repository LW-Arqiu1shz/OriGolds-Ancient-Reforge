package cn.blockforge.generated.origoldsar;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class OrigoldAnvilEvents {
    /** 本体模组 RangedCriticalModule 写在箭实体 NBT 上的空中暴击标记：玩家腾空（离地、未骑乘、不在水中、未爬梯）射击时由本体置为 true。 */
    private static final String AIR_CRIT_KEY = "origolds_air_critical";

    private OrigoldAnvilEvents() {
    }

    @SubscribeEvent
    public static void transferInfusedTraits(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.isEmpty()
                || right.isEmpty()
                || !TraitLibrary.isGildedAtLeast(left, 3)
                || !right.is(GeneratedMod.BROKEN_ORIGOLD_PARTS.get())
                || !TraitLibrary.isInfused(right)) {
            return;
        }

        ItemStack output = left.copyWithCount(1);
        TraitLibrary.mergeTraits(output, right);
        TraitLibrary.refreshTraitEffects(output);
        event.setOutput(output);
        event.setXpCost(1);
        event.setMaterialCost(1);
    }

    @SubscribeEvent
    public static void applyArmorPenetration(LivingIncomingDamageEvent event) {
        ItemStack weapon;
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow) {
            // DamageSource.weapon is not guaranteed to retain the bow/crossbow;
            // the arrow stores the weapon that fired it.
            // getWeaponItem() 可能为 null（发射器射出的箭、旧存档里的箭都没有记录武器），
            // 直接 .isEmpty() 会在伤害事件里抛空指针、把整个服务器带崩。
            weapon = arrow.getWeaponItem();
            if (weapon == null || weapon.isEmpty()) {
                weapon = getAttackingWeapon(event.getSource());
            }
        } else {
            weapon = getAttackingWeapon(event.getSource());
        }
        if (weapon.isEmpty()
                || !TraitLibrary.isGildedAtLeast(weapon, 3)
                || !TraitLibrary.isInfused(weapon)) {
            return;
        }

        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow) {
            float multiplier = 1.0F + TraitLibrary.getTraitValue(weapon, TraitLibrary.ATTACK_TRAIT) / 10000.0F;
            // 暴击判定与本体 RangedCriticalModule 同步：不再把“拉满弓/弩”视为暴击，
            // 只有玩家腾空射击、由本体打上空中暴击标记的箭才触发暴击伤害词条增伤。
            if (arrow.getPersistentData().getBooleanOr(AIR_CRIT_KEY, false)) {
                // 本体已在本事件上对空中暴击的箭乘过 1.5。暴击乘区与近战同步：
                // 基础暴击伤害 50%（即 1.5 倍），暴伤词条与它加法叠加进同一乘区，
                // 把 1.5 换算成 (1.5 + 词条)，而不是在 1.5 之上再乘 (1 + 词条)——
                // 否则 100% 暴伤词条会把 150% 直接翻倍成 300%，而非提升到 250%。
                float critDamage = TraitLibrary.getTraitValue(weapon, TraitLibrary.CRIT_DAMAGE_TRAIT) / 10000.0F;
                multiplier *= (1.5F + critDamage) / 1.5F;
            }
            if (multiplier != 1.0F) {
                event.setAmount(event.getAmount() * multiplier);
            }
        }

        int value = TraitLibrary.getTraitValue(weapon, TraitLibrary.PENETRATION_TRAIT);
        if (value <= 0) {
            return;
        }

        float penetration = Math.max(0.0F, Math.min(1.0F, value / 10000.0F));
        event.addReductionModifier(
                DamageContainer.Reduction.ARMOR,
                (container, armorReduction) -> armorReduction * (1.0F - penetration));
        event.addReductionModifier(
                DamageContainer.Reduction.ENCHANTMENTS,
                (container, enchantmentReduction) -> enchantmentReduction * (1.0F - penetration));
    }

    private static ItemStack getAttackingWeapon(DamageSource source) {
        ItemStack weapon = source.getWeaponItem();
        if (weapon != null && !weapon.isEmpty()) {
            return weapon;
        }

        if (source.getEntity() instanceof LivingEntity attacker) {
            return attacker.getMainHandItem();
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void addAttackTrait(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!TraitLibrary.isGildedAtLeast(stack, 3) || !TraitLibrary.isInfused(stack)) {
            return;
        }

        int value = TraitLibrary.getTraitValue(stack, TraitLibrary.ATTACK_TRAIT);
        if (value > 0) {
            event.addModifier(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            TraitLibrary.ATTACK_MODIFIER_ID,
                            value / 10000.0D,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                    EquipmentSlotGroup.HAND);
        }
    }

    @SubscribeEvent
    public static void addArmorTraits(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!TraitLibrary.isGildedAtLeast(stack, 3) || !TraitLibrary.isInfused(stack)
                || !isArmor(stack)) {
            return;
        }
        int armor = TraitLibrary.getTraitValue(stack, TraitLibrary.ARMOR_TRAIT);
        if (armor > 0) {
            // 直接按物品原生护甲值的比例相乘：+100% 即基础护甲值额外增加 100%。
            double armorMultiplier = armor / 10000.0D;
            event.addModifier(Attributes.ARMOR,
                    new AttributeModifier(TraitLibrary.ARMOR_MODIFIER_ID, armorMultiplier,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.ARMOR);
        }
        int toughness = TraitLibrary.getTraitValue(stack, TraitLibrary.ARMOR_TOUGHNESS_TRAIT);
        if (toughness > 0) {
            // 取“物品盔甲韧性 × 词条大小”与 0.2 的较大值：即使盔甲没有护甲韧性，词条也至少加 0.2。
            double toughnessBoost = Math.max(armorToughness(stack) * (toughness / 10000.0D), 0.2D);
            event.addModifier(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(TraitLibrary.ARMOR_TOUGHNESS_MODIFIER_ID, toughnessBoost,
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ARMOR);
        }
    }

    /**
     * 26.1.2 起原版没有 {@code ArmorItem} 类，护甲完全由数据组件描述：
     * 带 {@code EQUIPPABLE} 组件且槽位不是主手/副手的物品即为可穿戴护甲。
     */
    private static boolean isArmor(ItemStack stack) {
        net.minecraft.world.item.equipment.Equippable equippable =
                stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot().isArmor();
    }

    /** 物品原生护甲韧性：取属性修饰符里 ARMOR_TOUGHNESS 的加成值，没有则为 0。 */
    private static double armorToughness(ItemStack stack) {
        net.minecraft.world.item.component.ItemAttributeModifiers modifiers =
                stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0.0D;
        }
        double[] toughness = {0.0D};
        modifiers.forEach(net.minecraft.world.entity.EquipmentSlotGroup.ARMOR,
                (attribute, modifier) -> {
                    if (attribute.is(Attributes.ARMOR_TOUGHNESS)
                            && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                        toughness[0] = Math.max(toughness[0], modifier.amount());
                    }
                });
        return toughness[0];
    }

    @SubscribeEvent
    public static void applyToolEfficiency(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (!TraitLibrary.isGildedAtLeast(stack, 3) || !TraitLibrary.isInfused(stack)) {
            return;
        }

        int value = TraitLibrary.getTraitValue(stack, TraitLibrary.TOOL_EFFICIENCY_TRAIT);
        if (value > 0) {
            event.setNewSpeed(event.getNewSpeed() * (1.0F + value / 10000.0F));
        }
    }

    @SubscribeEvent
    public static void applyCriticalDamage(CriticalHitEvent event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (!event.isCriticalHit()
                || !TraitLibrary.isGildedAtLeast(stack, 3)
                || !TraitLibrary.isInfused(stack)) {
            return;
        }

        int value = TraitLibrary.getTraitValue(stack, TraitLibrary.CRIT_DAMAGE_TRAIT);
        if (value > 0) {
            event.setDamageMultiplier(event.getDamageMultiplier() + value / 10000.0F);
        }
    }

    @SubscribeEvent
    public static void washGilding(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        var level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        ItemStack held = player.getItemInHand(event.getHand());
        if (!state.is(Blocks.WATER_CAULDRON)
                || state.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MIN_FILL_LEVEL
                || held.isEmpty()
                || TraitLibrary.getGildingLevel(held) <= 0
                || TraitLibrary.isInfused(held)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult((level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER));
        if (level.isClientSide()) {
            return;
        }

        ItemStack washed = held.getCount() > 1 ? held.split(1) : held;
        TraitLibrary.lowerGildingLevel(washed);
        if (washed != held) {
            player.getInventory().placeItemBackInInventory(washed);
        }
        LayeredCauldronBlock.lowerFillLevel(state, level, event.getPos());
        player.awardStat(Stats.USE_CAULDRON);
    }
}
