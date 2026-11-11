package cn.blockforge.generated.origoldsar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 永恒药水的合成配方：以「+」形摆放 4 个魔金之核（origolds:magigold_core），
 * 中心放任意药水（饮用/喷溅/滞留，含水瓶等无效果药水）。
 */
public final class EternalPotionRecipe extends CustomRecipe {
    public EternalPotionRecipe() {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = input.getItem(slot);
            if (slot == 4) {
                if (!EternalPotionItem.isPotion(stack)) {
                    return false;
                }
            } else if (slot == 1 || slot == 3 || slot == 5 || slot == 7) {
                if (!OrigoldsIngredients.isMagigoldCore(stack)) {
                    return false;
                }
            } else if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack potion = input.getItem(4);
        return EternalPotionItem.isPotion(potion) ? EternalPotionItem.create(potion) : ItemStack.EMPTY;
    }

                @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return (RecipeSerializer<? extends CustomRecipe>) GeneratedMod.ETERNAL_POTION_RECIPE_SERIALIZER.get();
    }

        private static final EternalPotionRecipe INSTANCE = new EternalPotionRecipe();
    public static final MapCodec<EternalPotionRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, EternalPotionRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<EternalPotionRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
}
