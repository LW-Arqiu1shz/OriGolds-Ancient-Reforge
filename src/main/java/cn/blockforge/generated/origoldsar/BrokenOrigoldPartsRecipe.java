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
    public BrokenOrigoldPartsRecipe() {}

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
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(GeneratedMod.BROKEN_ORIGOLD_PARTS.get(), 2);
    }

                @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return (RecipeSerializer<? extends CustomRecipe>) GeneratedMod.BROKEN_ORIGOLD_PARTS_RECIPE_SERIALIZER.get();
    }

        private static boolean isGildedItem(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData =
                stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return customData != null
                && customData.contains("origolds_gilding_level")
                && customData.copyTag().getIntOr("origolds_gilding_level", 0) >= 3;
    }

    private static final BrokenOrigoldPartsRecipe INSTANCE = new BrokenOrigoldPartsRecipe();
    public static final MapCodec<BrokenOrigoldPartsRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, BrokenOrigoldPartsRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<BrokenOrigoldPartsRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
}
