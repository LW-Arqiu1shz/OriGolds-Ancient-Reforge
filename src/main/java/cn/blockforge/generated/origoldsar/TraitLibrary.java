package cn.blockforge.generated.origoldsar;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TraitLibrary {
    public static final int ALLOCATION_COUNT = 3;
    public static final int TRAITS_PER_ALLOCATION = 3;
    public static final int ATTACK_TRAIT = 0;
    public static final int CRIT_DAMAGE_TRAIT = 1;
    public static final int DURABILITY_TRAIT = 2;
    public static final int TOOL_EFFICIENCY_TRAIT = 3;
    public static final int PENETRATION_TRAIT = 4;
    public static final int ARMOR_TRAIT = 5;
    public static final int ARMOR_TOUGHNESS_TRAIT = 6;
    private static final int[][] ALLOCATIONS = {
            {0, 2, 5},
            {0, 1, 4},
            {1, 2, 6}
    };
    private static final String[] TRAIT_KEYS = {
            "attack", "crit_damage", "durability", "tool_efficiency", "penetration",
            "armor", "armor_toughness"
    };
    private static final int[] MIN_VALUES = {2000, 2000, 2000, 2000, 1000, 2000, 2000};
    private static final int[] MAX_VALUES = {10000, 10000, 10000, 10000, 5000, 10000, 10000};
    private static final String TRAITS_KEY = "origolds_ar_traits";
    private static final String TRAIT_VALUES_KEY = "origolds_ar_trait_values";
    private static final String BASE_MAX_DAMAGE_KEY = "origolds_ar_trait_base_max_damage";
    public static final String GILDING_LEVEL_KEY = "origolds_gilding_level";
    public static final Identifier ATTACK_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "trait_attack");
    public static final Identifier ARMOR_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "trait_armor");
    public static final Identifier ARMOR_TOUGHNESS_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "trait_armor_toughness");

    private TraitLibrary() {
    }

    public static int[] allocation(int option) {
        return ALLOCATIONS[Math.floorMod(option, ALLOCATION_COUNT)].clone();
    }

    /**
     * Generates a fresh three-trait combination containing the selected trait.
     * The two companion traits are shuffled on every infusion operation so that
     * repeatedly selecting the same displayed trait does not produce a fixed set.
     */
    public static int[] randomAllocationContaining(int selectedTrait) {
        if (selectedTrait < 0 || selectedTrait >= TRAIT_KEYS.length) {
            return allocation(0);
        }
        List<Integer> companions = new ArrayList<>();
        for (int trait = 0; trait < TRAIT_KEYS.length; trait++) {
            if (trait != selectedTrait) {
                companions.add(trait);
            }
        }
        Collections.shuffle(companions);
        int[] result = {selectedTrait, companions.get(0), companions.get(1)};
        return result;
    }

    public static int primaryTrait(int option) {
        return allocation(option)[0];
    }

    public static String name(int id) {
        return id >= 0 && id < TRAIT_KEYS.length ? TRAIT_KEYS[id] : "unknown";
    }

    public static int idFromCommandName(String commandName) {
        return switch (commandName) {
            case "atk" -> ATTACK_TRAIT;
            case "critdmg" -> CRIT_DAMAGE_TRAIT;
            case "dur" -> DURABILITY_TRAIT;
            case "tooleff" -> TOOL_EFFICIENCY_TRAIT;
            case "pen" -> PENETRATION_TRAIT;
            case "arm" -> ARMOR_TRAIT;
            case "armt" -> ARMOR_TOUGHNESS_TRAIT;
            default -> -1;
        };
    }

    public static boolean isValidCommandSize(int trait, double size) {
        return trait >= 0 && trait < TRAIT_KEYS.length && size >= 0.01D && size <= 6553500D;
    }

    public static Component component(int id) {
        return Component.translatable("trait.origolds_ar." + name(id));
    }

    public static Component optionComponent(int id) {
        return component(id).copy().append(Component.translatable("trait.origolds_ar.option_suffix"));
    }

    public static Component allocationComponent(int[] traits) {
        if (traits.length == 0) {
            return Component.empty();
        }
        Component result = component(traits[0]);
        for (int i = 1; i < traits.length; i++) {
            result = result.copy().append(" · ").append(component(traits[i]));
        }
        return result;
    }

    public static void writeSelectedTraits(ItemStack stack, int[] traits) {
        int[] selectedTraits = traits.clone();
        int[] values = new int[selectedTraits.length];
        int[] tiers = new int[selectedTraits.length];
        RandomSource random = RandomSource.create();
        boolean hasEpic = false;

        for (int i = 0; i < selectedTraits.length; i++) {
            tiers[i] = randomTier(random);
            hasEpic |= tiers[i] == 2;
        }
        if (!hasEpic && selectedTraits.length > 0) {
            tiers[random.nextInt(selectedTraits.length)] = 2;
        }
        for (int i = 0; i < selectedTraits.length; i++) {
            values[i] = randomValueInTier(random, selectedTraits[i], tiers[i]);
        }

        writeTraitData(stack, selectedTraits, values);
    }

    public static void copyTraits(ItemStack source, ItemStack target) {
        writeTraitData(target, readTraits(source), readTraitValues(source));
    }

    public static void clearTraits(ItemStack stack) {
        int baseMaxDamage = getOrCaptureBaseMaxDamage(stack);
        net.minecraft.world.item.component.CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.remove(TRAITS_KEY);
                    tag.remove(TRAIT_VALUES_KEY);
                    tag.remove(BASE_MAX_DAMAGE_KEY);
                });
        if (baseMaxDamage > 0) {
            stack.set(DataComponents.MAX_DAMAGE, baseMaxDamage);
        }
        if (stack.is(GeneratedMod.BROKEN_ORIGOLD_PARTS.get())) {
            stack.remove(DataComponents.CUSTOM_NAME);
        }
    }

    public static boolean hasTrait(ItemStack stack, int trait) {
        return getTraitValue(stack, trait) > 0;
    }

    public static boolean installTrait(ItemStack stack, int trait, double percentage) {
        if (!isValidCommandSize(trait, percentage) || hasTrait(stack, trait)) {
            return false;
        }
        return replaceTrait(stack, trait, percentage);
    }

    public static boolean removeTrait(ItemStack stack, int trait) {
        if (!hasTrait(stack, trait)) {
            return false;
        }
        return replaceTrait(stack, trait, 0);
    }

    public static boolean setTrait(ItemStack stack, int trait, double percentage) {
        if (!isValidCommandSize(trait, percentage) || !hasTrait(stack, trait)) {
            return false;
        }
        return replaceTrait(stack, trait, percentage);
    }

    private static boolean replaceTrait(ItemStack stack, int trait, double percentage) {
        int[] traits = readTraits(stack);
        int[] values = readTraitValues(stack);
        int[] expandedValues = new int[TRAIT_KEYS.length];
        for (int index = 0; index < traits.length && index < values.length; index++) {
            if (traits[index] >= 0 && traits[index] < expandedValues.length) {
                expandedValues[traits[index]] = Math.max(expandedValues[traits[index]], values[index]);
            }
        }
        expandedValues[trait] = (int) Math.round(percentage * 100D);

        int traitCount = 0;
        for (int value : expandedValues) {
            if (value > 0) {
                traitCount++;
            }
        }
        if (traitCount == 0) {
            clearTraits(stack);
            return true;
        }

        int[] compactTraits = new int[traitCount];
        int[] compactValues = new int[traitCount];
        int outputIndex = 0;
        for (int index = 0; index < expandedValues.length; index++) {
            if (expandedValues[index] > 0) {
                compactTraits[outputIndex] = index;
                compactValues[outputIndex] = expandedValues[index];
                outputIndex++;
            }
        }
        int baseMaxDamage = getOrCaptureBaseMaxDamage(stack);
        net.minecraft.world.item.component.CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putIntArray(TRAITS_KEY, compactTraits);
                    tag.putIntArray(TRAIT_VALUES_KEY, compactValues);
                });
        setMaxDamageFromTraits(stack, baseMaxDamage, compactTraits, compactValues);
        return true;
    }

    private static void writeTraitData(ItemStack stack, int[] traits, int[] values) {
        net.minecraft.world.item.component.CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putIntArray(TRAITS_KEY, traits.clone());
                    tag.putIntArray(TRAIT_VALUES_KEY, values.clone());
                });
        stack.set(DataComponents.CUSTOM_NAME,
                Component.translatable(
                                "item.origolds_ar.infused_origold_parts_suffix",
                                component(mainTrait(traits, values)))
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA).withItalic(false)));
    }

    private static int randomTier(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 40) {
            return 0;
        }
        if (roll < 70) {
            return 1;
        }
        if (roll < 90) {
            return 2;
        }
        return 3;
    }

    private static int randomValueInTier(RandomSource random, int trait, int tier) {
        int min = MIN_VALUES[trait];
        int max = MAX_VALUES[trait];
        int quarter = (max - min) / 4;
        int lower = min + tier * quarter;
        int upper = tier == 3 ? max : lower + quarter - 1;
        return random.nextInt(upper - lower + 1) + lower;
    }

    /**
     * 占比最高的词条（决定碎片名称）。只看合法词条 id、且两数组都有值的下标：
     * 物品 NBT 可能被命令/别的模组改坏（空数组、数组长度不一、越界 id），
     * 这里一律跳过，绝不能在合成结果计算里抛越界异常把服务器带崩。
     * 一个合法词条都没有时返回 -1（名称显示为“未知”）。
     */
    private static int mainTrait(int[] traits, int[] values) {
        int mainIndex = -1;
        for (int i = 0; i < traits.length && i < values.length; i++) {
            int currentTrait = traits[i];
            if (!isValidTrait(currentTrait)) {
                continue;
            }
            if (mainIndex < 0) {
                mainIndex = i;
                continue;
            }
            int bestTrait = traits[mainIndex];
            if ((long) values[i] * MAX_VALUES[bestTrait] > (long) values[mainIndex] * MAX_VALUES[currentTrait]) {
                mainIndex = i;
            }
        }
        return mainIndex < 0 ? -1 : traits[mainIndex];
    }

    private static boolean isValidTrait(int trait) {
        return trait >= 0 && trait < TRAIT_KEYS.length;
    }

    public static boolean isInfused(ItemStack stack) {
        CompoundTag tag = traitTag(stack);
        if (tag == null) {
            return false;
        }
        int[] traits = intArray(tag, TRAITS_KEY);
        int[] values = intArray(tag, TRAIT_VALUES_KEY);
        return traits.length > 0 && values.length >= traits.length;
    }

    public static int getGildingLevel(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(GILDING_LEVEL_KEY)) {
            return 0;
        }
        return Math.max(0, customData.copyTag().getIntOr(GILDING_LEVEL_KEY, 0));
    }

    public static boolean isGildedAtLeast(ItemStack stack, int requiredLevel) {
        return getGildingLevel(stack) >= requiredLevel;
    }

    public static boolean lowerGildingLevel(ItemStack stack) {
        int level = getGildingLevel(stack);
        if (level <= 0) {
            return false;
        }
        net.minecraft.world.item.component.CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    if (level <= 1) {
                        tag.remove(GILDING_LEVEL_KEY);
                    } else {
                        tag.putInt(GILDING_LEVEL_KEY, level - 1);
                    }
                });
        return true;
    }

    public static void mergeTraits(ItemStack target, ItemStack source) {
        int baseMaxDamage = getOrCaptureBaseMaxDamage(target);
        int[] targetTraits = readTraits(target);
        int[] targetValues = readTraitValues(target);
        int[] sourceTraits = readTraits(source);
        int[] sourceValues = readTraitValues(source);
        int[] mergedValues = new int[TRAIT_KEYS.length];
        boolean[] present = new boolean[TRAIT_KEYS.length];

        for (int index = 0; index < targetTraits.length && index < targetValues.length; index++) {
            int trait = targetTraits[index];
            if (trait >= 0 && trait < mergedValues.length && targetValues[index] > 0) {
                present[trait] = true;
                mergedValues[trait] = Math.max(mergedValues[trait], targetValues[index]);
            }
        }

        // Existing traits are anchored. Once there are three, a new source trait
        // cannot displace any of them; it can only strengthen a matching trait.
        int traitCount = 0;
        for (boolean value : present) {
            if (value) {
                traitCount++;
            }
        }
        for (int index = 0; index < sourceTraits.length && index < sourceValues.length; index++) {
            int trait = sourceTraits[index];
            if (trait < 0 || trait >= mergedValues.length || sourceValues[index] <= 0) {
                continue;
            }
            if (present[trait]) {
                mergedValues[trait] = Math.max(mergedValues[trait], sourceValues[index]);
            } else if (traitCount < TRAITS_PER_ALLOCATION) {
                present[trait] = true;
                mergedValues[trait] = sourceValues[index];
                traitCount++;
            }
        }

        int[] mergedTraits = new int[traitCount];
        int[] compactValues = new int[traitCount];
        int outputIndex = 0;
        for (int trait : targetTraits) {
            if (trait >= 0 && trait < mergedValues.length && present[trait]) {
                mergedTraits[outputIndex] = trait;
                compactValues[outputIndex] = mergedValues[trait];
                outputIndex++;
                present[trait] = false;
            }
        }
        for (int trait = 0; trait < mergedValues.length; trait++) {
            if (present[trait]) {
                mergedTraits[outputIndex] = trait;
                compactValues[outputIndex] = mergedValues[trait];
                outputIndex++;
            }
        }

        net.minecraft.world.item.component.CustomData.update(
                DataComponents.CUSTOM_DATA,
                target,
                tag -> {
                    tag.putIntArray(TRAITS_KEY, mergedTraits);
                    tag.putIntArray(TRAIT_VALUES_KEY, compactValues);
                });
        setMaxDamageFromTraits(target, baseMaxDamage, mergedTraits, compactValues);
    }

    private static int getOrCaptureBaseMaxDamage(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(BASE_MAX_DAMAGE_KEY)) {
                return tag.getIntOr(BASE_MAX_DAMAGE_KEY, 0);
            }
        }

        Integer maxDamage = stack.get(DataComponents.MAX_DAMAGE);
        int baseMaxDamage = maxDamage == null ? 0 : maxDamage;
        if (baseMaxDamage > 0) {
            net.minecraft.world.item.component.CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    stack,
                    tag -> tag.putInt(BASE_MAX_DAMAGE_KEY, baseMaxDamage));
        }
        return baseMaxDamage;
    }

    private static void setMaxDamageFromTraits(ItemStack stack, int baseMaxDamage, int[] traits, int[] values) {
        if (baseMaxDamage <= 0) {
            return;
        }
        int durability = 0;
        for (int index = 0; index < traits.length && index < values.length; index++) {
            if (traits[index] == DURABILITY_TRAIT) {
                durability = Math.max(durability, values[index]);
            }
        }
        long scaledMaxDamage = Math.round(baseMaxDamage * (1.0D + durability / 10000.0D));
        stack.set(DataComponents.MAX_DAMAGE, (int) Math.min(Integer.MAX_VALUE, scaledMaxDamage));
    }

    public static void refreshTraitEffects(ItemStack stack) {
        if (!isInfused(stack)) {
            return;
        }
        int[] traits = readTraits(stack);
        int[] values = readTraitValues(stack);
        setMaxDamageFromTraits(stack, getOrCaptureBaseMaxDamage(stack), traits, values);
    }

    public static int getTraitValue(ItemStack stack, int trait) {
        CompoundTag tag = traitTag(stack);
        if (tag == null) {
            return 0;
        }
        int[] traits = intArray(tag, TRAITS_KEY);
        int[] values = intArray(tag, TRAIT_VALUES_KEY);
        for (int index = 0; index < traits.length && index < values.length; index++) {
            if (traits[index] == trait) {
                return values[index];
            }
        }
        return 0;
    }

    public static int[] readTraits(ItemStack stack) {
        CompoundTag tag = traitTag(stack);
        return tag == null ? EMPTY_INTS : intArray(tag, TRAITS_KEY);
    }

    public static int[] readTraitValues(ItemStack stack) {
        CompoundTag tag = traitTag(stack);
        return tag == null ? EMPTY_INTS : intArray(tag, TRAIT_VALUES_KEY);
    }

    private static final int[] EMPTY_INTS = new int[0];

    /**
     * 带词条的物品才复制一次自定义数据。
     *
     * <p>性能：伤害、挖掘速度、属性修饰符等事件对每一件装备都会来问词条，绝大多数物品根本
     * 没有词条；{@code CustomData#contains} 不复制 NBT，先用它挡掉，避免每次都整份
     * {@code copyTag()}（以前一次查询要复制两遍）。
     */
    private static CompoundTag traitTag(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(TRAITS_KEY)) {
            return null;
        }
        return customData.copyTag();
    }

    private static int[] intArray(CompoundTag tag, String key) {
        return tag.getIntArray(key).orElse(EMPTY_INTS);
    }

    public static ChatFormatting tierFormatting(int trait, int value) {
        if (!isValidTrait(trait)) {
            // 被改坏的词条 id：照常显示，不能在悬停提示里越界把客户端崩掉。
            return ChatFormatting.GRAY;
        }
        int min = MIN_VALUES[trait];
        int max = MAX_VALUES[trait];
        int quarter = (max - min) / 4;
        if (value < min + quarter) {
            return ChatFormatting.WHITE;
        }
        if (value < min + quarter * 2) {
            return ChatFormatting.GREEN;
        }
        if (value < min + quarter * 3) {
            return ChatFormatting.LIGHT_PURPLE;
        }
        return ChatFormatting.GOLD;
    }

    public static String formatPercentage(int value) {
        return (value / 100) + "." + (value % 100 < 10 ? "0" : "") + (value % 100) + "%";
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        int[] traits = readTraits(stack);
        int[] values = readTraitValues(stack);
        for (int i = 0; i < traits.length; i++) {
            int trait = traits[i];
            int value = i < values.length ? values[i] : 0;
            tooltip.add(Component.literal("- ").append(component(trait)).append(" +")
                    .append(Component.literal(formatPercentage(value)))
                    .withStyle(tierFormatting(trait, value)));
        }
    }
}
