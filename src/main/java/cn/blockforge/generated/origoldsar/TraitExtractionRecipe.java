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
    public TraitExtractionRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int equipmentSlot = findEquipmentSlot(input);
        int partsSlot = findPartsSlot(input);
        return equipmentSlot >= 0 && partsSlot >= 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, net.minecraft.core.HolderLookup.Provider registries) {
        int equipmentSlot = findEquipmentSlot(input);
        if (equipmentSlot < 0) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(GeneratedMod.BROKEN_ORIGOLD_PARTS.get());
        TraitLibrary.copyTraits(input.getItem(equipmentSlot), result);
        return result;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return new ItemStack(GeneratedMod.BROKEN_ORIGOLD_PARTS.get());
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
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(GeneratedMod.BROKEN_ORIGOLD_PARTS.get()));
        ingredients.add(Ingredient.of(ItemStack.EMPTY));
        return ingredients;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GeneratedMod.TRAIT_EXTRACTION_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
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

    public static final class Serializer implements RecipeSerializer<TraitExtractionRecipe> {
        private static final MapCodec<TraitExtractionRecipe> CODEC =
                MapCodec.unit(TraitExtractionRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, TraitExtractionRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buffer, recipe) -> {},
                        buffer -> new TraitExtractionRecipe());

        @Override
        public MapCodec<TraitExtractionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TraitExtractionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
