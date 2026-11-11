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

public final class TraitRemovalRecipe extends CustomRecipe {
    public TraitRemovalRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int found = findInputSlot(input);
        return found >= 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, net.minecraft.core.HolderLookup.Provider registries) {
        int slot = findInputSlot(input);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = input.getItem(slot).copyWithCount(1);
        TraitLibrary.clearTraits(result);
        return result;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(ItemStack.EMPTY));
        return ingredients;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GeneratedMod.TRAIT_REMOVAL_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    private static int findInputSlot(CraftingInput input) {
        int found = -1;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (found >= 0 || !TraitLibrary.isGildedAtLeast(stack, 1) || !TraitLibrary.isInfused(stack)) {
                return -1;
            }
            found = slot;
        }
        return found;
    }

    public static final class Serializer implements RecipeSerializer<TraitRemovalRecipe> {
        private static final MapCodec<TraitRemovalRecipe> CODEC =
                MapCodec.unit(TraitRemovalRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, TraitRemovalRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buffer, recipe) -> {},
                        buffer -> new TraitRemovalRecipe());

        @Override
        public MapCodec<TraitRemovalRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TraitRemovalRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
