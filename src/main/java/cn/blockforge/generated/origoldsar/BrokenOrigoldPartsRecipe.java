package cn.blockforge.generated.origoldsar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class BrokenOrigoldPartsRecipe extends CustomRecipe {
    public BrokenOrigoldPartsRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasGildedItem = false;
        boolean hasStoneMaterial = false;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (isGildedItem(stack) && !hasGildedItem) {
                hasGildedItem = true;
            } else if (stack.is(ItemTags.STONE_CRAFTING_MATERIALS) && !hasStoneMaterial) {
                hasStoneMaterial = true;
            } else {
                return false;
            }
        }

        return hasGildedItem && hasStoneMaterial;
    }

    @Override
    public ItemStack assemble(CraftingInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return new ItemStack(GeneratedMod.BROKEN_ORIGOLD_PARTS.get(), 2);
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return new ItemStack(GeneratedMod.BROKEN_ORIGOLD_PARTS.get(), 2);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(ItemTags.DURABILITY_ENCHANTABLE));
        ingredients.add(Ingredient.of(ItemTags.STONE_CRAFTING_MATERIALS));
        return ingredients;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GeneratedMod.BROKEN_ORIGOLD_PARTS_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    private static boolean isGildedItem(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData =
                stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return customData != null
                && customData.contains("origolds_gilding_level")
                && customData.copyTag().getInt("origolds_gilding_level") >= 3;
    }

    public static final class Serializer implements RecipeSerializer<BrokenOrigoldPartsRecipe> {
        private static final MapCodec<BrokenOrigoldPartsRecipe> CODEC =
                MapCodec.unit(BrokenOrigoldPartsRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, BrokenOrigoldPartsRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buffer, recipe) -> {},
                        buffer -> new BrokenOrigoldPartsRecipe());

        @Override
        public MapCodec<BrokenOrigoldPartsRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BrokenOrigoldPartsRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
