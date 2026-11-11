package cn.blockforge.generated.origoldsar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class TraitExtractionRecipe extends CustomRecipe {
    public TraitExtractionRecipe() {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int equipmentSlot = findEquipmentSlot(input);
        int partsSlot = findPartsSlot(input);
        return equipmentSlot >= 0 && partsSlot >= 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        int equipmentSlot = findEquipmentSlot(input);
        if (equipmentSlot < 0) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(GeneratedMod.BROKEN_ORIGOLD_PARTS.get());
        TraitLibrary.copyTraits(input.getItem(equipmentSlot), result);
        return result;
    }

        @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        int equipmentSlot = findEquipmentSlot(input);
        if (equipmentSlot >= 0) {
            ItemStack restoredEquipment = input.getItem(equipmentSlot).copyWithCount(1);
            TraitLibrary.clearTraits(restoredEquipment);
            remaining.set(equipmentSlot, restoredEquipment);
        }
        return remaining;
    }

            @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return (RecipeSerializer<? extends CustomRecipe>) GeneratedMod.TRAIT_EXTRACTION_RECIPE_SERIALIZER.get();
    }

        private static int findEquipmentSlot(CraftingInput input) {
        int found = -1;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (TraitLibrary.isGildedAtLeast(stack, 3) && TraitLibrary.isInfused(stack)) {
                if (found >= 0) {
                    return -1;
                }
                found = slot;
            }
        }
        return found;
    }

    private static int findPartsSlot(CraftingInput input) {
        int found = -1;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(GeneratedMod.BROKEN_ORIGOLD_PARTS.get()) && !TraitLibrary.isInfused(stack)) {
                if (found >= 0) {
                    return -1;
                }
                found = slot;
            } else if (slot != findEquipmentSlot(input)) {
                return -1;
            }
        }
        return found;
    }

    private static final TraitExtractionRecipe INSTANCE = new TraitExtractionRecipe();
    public static final MapCodec<TraitExtractionRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, TraitExtractionRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<TraitExtractionRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
}
