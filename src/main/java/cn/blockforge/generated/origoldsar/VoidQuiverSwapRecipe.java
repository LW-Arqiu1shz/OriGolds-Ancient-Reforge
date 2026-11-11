package cn.blockforge.generated.origoldsar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 虚空箭匣换内容配方（工作台无序）：1 个虚空箭匣 + 恰好 4 个同种类的箭矢或烟花火箭 →
 * 内容被替换为新内容的虚空箭匣（例如：虚空迟缓之箭箭匣 + 4 根光灵箭 → 虚空光灵箭箭匣）。
 */
public final class VoidQuiverSwapRecipe extends CustomRecipe {
    public VoidQuiverSwapRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasQuiver = false;
        ItemStack firstArrow = ItemStack.EMPTY;
        int arrowTotal = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!hasQuiver && stack.is(GeneratedMod.VOID_QUIVER.get())) {
                hasQuiver = true;
            } else if (VoidQuiverItem.isProjectile(stack)) {
                if (firstArrow.isEmpty()) {
                    firstArrow = stack;
                } else if (!ItemStack.isSameItemSameComponents(firstArrow, stack)) {
                    return false;
                }
                arrowTotal += stack.getCount();
            } else {
                return false;
            }
        }
        return hasQuiver && arrowTotal == 4;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (VoidQuiverItem.isProjectile(stack)) {
                return VoidQuiverItem.create(stack);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return VoidQuiverItem.create(new ItemStack(Items.ARROW));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(GeneratedMod.VOID_QUIVER.get()));
        ingredients.add(Ingredient.of(net.minecraft.tags.ItemTags.ARROWS));
        return ingredients;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 5;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GeneratedMod.VOID_QUIVER_SWAP_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    public static final class Serializer implements RecipeSerializer<VoidQuiverSwapRecipe> {
        private static final MapCodec<VoidQuiverSwapRecipe> CODEC = MapCodec.unit(VoidQuiverSwapRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, VoidQuiverSwapRecipe> STREAM_CODEC =
                StreamCodec.of((buffer, recipe) -> {}, buffer -> new VoidQuiverSwapRecipe());

        @Override
        public MapCodec<VoidQuiverSwapRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VoidQuiverSwapRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
