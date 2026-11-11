package cn.blockforge.generated.origoldsar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 永恒药水的合成配方：以「+」形摆放 4 个魔金之核（origolds:magigold_core），
 * 中心放任意药水（饮用/喷溅/滞留，含水瓶等无效果药水）。
 */
public final class EternalPotionRecipe extends CustomRecipe {
    private static final Ingredient CORE = BuiltInRegistries.ITEM
            .getOptional(ResourceLocation.fromNamespaceAndPath("origolds", "magigold_core"))
            .<Ingredient>map(Ingredient::of)
            .orElseGet(() -> Ingredient.of(net.minecraft.world.item.Items.AIR));

    public EternalPotionRecipe() {
        super(CraftingBookCategory.MISC);
    }

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
                if (!CORE.test(stack)) {
                    return false;
                }
            } else if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack potion = input.getItem(4);
        return EternalPotionItem.isPotion(potion) ? EternalPotionItem.create(potion) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return EternalPotionItem.create(new ItemStack(net.minecraft.world.item.Items.POTION));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(
                net.minecraft.world.item.Items.POTION,
                net.minecraft.world.item.Items.SPLASH_POTION,
                net.minecraft.world.item.Items.LINGERING_POTION));
        for (int i = 0; i < 4; i++) {
            ingredients.add(CORE);
        }
        return ingredients;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GeneratedMod.ETERNAL_POTION_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    public static final class Serializer implements RecipeSerializer<EternalPotionRecipe> {
        private static final MapCodec<EternalPotionRecipe> CODEC = MapCodec.unit(EternalPotionRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, EternalPotionRecipe> STREAM_CODEC =
                StreamCodec.of((buffer, recipe) -> {}, buffer -> new EternalPotionRecipe());

        @Override
        public MapCodec<EternalPotionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EternalPotionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
