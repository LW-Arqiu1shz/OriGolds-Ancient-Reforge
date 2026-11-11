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
import net.neoforged.neoforge.common.Tags;

/**
 * 虚空箭匣的合成配方（工作台 3x3）：
 * 【箭矢 魔金之核 箭矢 / 魔金之核 箱子或木桶 魔金之核 / 箭矢 魔金之核 箭矢】，
 * 四角必须是 4 根同种类箭矢（含药水箭、光灵箭等），中心为任意箱子或木桶。
 */
public final class VoidQuiverRecipe extends CustomRecipe {
    public VoidQuiverRecipe() {}

    private static boolean isContainer(ItemStack stack) {
        return stack.is(Tags.Items.CHESTS) || stack.is(Tags.Items.BARRELS);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }
        ItemStack firstArrow = ItemStack.EMPTY;
        int arrowTotal = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = input.getItem(slot);
            if (slot == 4) {
                if (!isContainer(stack)) {
                    return false;
                }
            } else if (slot == 0 || slot == 2 || slot == 6 || slot == 8) {
                if (!VoidQuiverItem.isProjectile(stack)) {
                    return false;
                }
                if (firstArrow.isEmpty()) {
                    firstArrow = stack;
                } else if (!ItemStack.isSameItemSameComponents(firstArrow, stack)) {
                    return false;
                }
                arrowTotal += stack.getCount();
            } else if (!OrigoldsIngredients.isMagigoldCore(stack)) {
                return false;
            }
        }
        return arrowTotal == 4;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack arrow = input.getItem(0);
        return VoidQuiverItem.isProjectile(arrow) ? VoidQuiverItem.create(arrow) : ItemStack.EMPTY;
    }

                @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return (RecipeSerializer<? extends CustomRecipe>) GeneratedMod.VOID_QUIVER_RECIPE_SERIALIZER.get();
    }

        private static final VoidQuiverRecipe INSTANCE = new VoidQuiverRecipe();
    public static final MapCodec<VoidQuiverRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, VoidQuiverRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<VoidQuiverRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
}
