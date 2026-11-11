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
 * 永恒药水换内容配方（工作台无序）：1 瓶永恒药水 + 任意药水 →
 * 内容被替换为新药水的永恒药水（例如：永恒喷溅型迅捷药水 + 滞留抗火药水 → 永恒滞留抗火药水）。
 */
public final class EternalPotionSwapRecipe extends CustomRecipe {
    public EternalPotionSwapRecipe() {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasEternal = false;
        boolean hasPotion = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!hasEternal && EternalPotionItem.isEternalPotion(stack)) {
                hasEternal = true;
            } else if (!hasPotion && EternalPotionItem.isPotion(stack)) {
                hasPotion = true;
            } else {
                return false;
            }
        }
        return hasEternal && hasPotion;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (EternalPotionItem.isPotion(stack)) {
                return EternalPotionItem.create(stack);
            }
        }
        return ItemStack.EMPTY;
    }

                @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return (RecipeSerializer<? extends CustomRecipe>) GeneratedMod.ETERNAL_POTION_SWAP_RECIPE_SERIALIZER.get();
    }

        private static final EternalPotionSwapRecipe INSTANCE = new EternalPotionSwapRecipe();
    public static final MapCodec<EternalPotionSwapRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, EternalPotionSwapRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<EternalPotionSwapRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
}
