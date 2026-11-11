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
    public TraitRemovalRecipe() {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int found = findInputSlot(input);
        return found >= 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        int slot = findInputSlot(input);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = input.getItem(slot).copyWithCount(1);
        TraitLibrary.clearTraits(result);
        return result;
    }

                @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return (RecipeSerializer<? extends CustomRecipe>) GeneratedMod.TRAIT_REMOVAL_RECIPE_SERIALIZER.get();
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

    private static final TraitRemovalRecipe INSTANCE = new TraitRemovalRecipe();
    public static final MapCodec<TraitRemovalRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, TraitRemovalRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<TraitRemovalRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
}
