package cn.blockforge.generated.origoldsar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

/**
 * 虚空箭匣的合成配方（工作台 3x3）：
 * 【箭矢 魔金之核 箭矢 / 魔金之核 箱子或木桶 魔金之核 / 箭矢 魔金之核 箭矢】，
 * 四角必须是 4 个同种类的箭矢或烟花火箭（含药水箭、光灵箭等），中心为任意箱子或木桶。
 */
public final class VoidQuiverRecipe extends CustomRecipe {
    private static final Ingredient CORE = BuiltInRegistries.ITEM
            .getOptional(ResourceLocation.fromNamespaceAndPath("origolds", "magigold_core"))
            .<Ingredient>map(Ingredient::of)
            .orElseGet(() -> Ingredient.of(Items.AIR));
    private static final Ingredient ARROWS = Ingredient.of(ItemTags.ARROWS);
    private static final Ingredient CONTAINERS = Ingredient.of(Items.CHEST, Items.TRAPPED_CHEST, Items.BARREL);

    public VoidQuiverRecipe() {
        super(CraftingBookCategory.MISC);
    }

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
            } else if (!CORE.test(stack)) {
                return false;
            }
        }
        return arrowTotal == 4;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack arrow = input.getItem(0);
        return VoidQuiverItem.isProjectile(arrow) ? VoidQuiverItem.create(arrow) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return VoidQuiverItem.create(new ItemStack(Items.ARROW));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(ARROWS);
        ingredients.add(ARROWS);
        ingredients.add(ARROWS);
        ingredients.add(ARROWS);
        for (int i = 0; i < 4; i++) {
            ingredients.add(CORE);
        }
        ingredients.add(CONTAINERS);
        return ingredients;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GeneratedMod.VOID_QUIVER_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    public static final class Serializer implements RecipeSerializer<VoidQuiverRecipe> {
        private static final MapCodec<VoidQuiverRecipe> CODEC = MapCodec.unit(VoidQuiverRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, VoidQuiverRecipe> STREAM_CODEC =
                StreamCodec.of((buffer, recipe) -> {}, buffer -> new VoidQuiverRecipe());

        @Override
        public MapCodec<VoidQuiverRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VoidQuiverRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
