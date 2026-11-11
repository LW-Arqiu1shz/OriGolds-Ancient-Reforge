package cn.blockforge.generated.origoldsar;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 前置模组《金源工艺》物品的惰性配方原料。
 *
 * <p>这里刻意不在静态初始化阶段解析注册表：配方类的 {@code SERIALIZER} 会在
 * {@code RegisterEvent}（RECIPE_SERIALIZER）期间被创建，而那一刻物品注册表里模组的物品
 * 可能还没注册完、标签也尚未绑定。此时 {@code getOrThrow(TagKey)} 会抛
 * {@code IllegalStateException("Missing tag ...")}，退而用空集合兜底又会撞上
 * {@link Ingredient} 的 "Ingredients can't be empty" / "Ingredient can't contain air" 校验，
 * 无论哪条都会让类初始化失败（{@code ExceptionInInitializerError}），直接崩在加载期。
 * 所以改成延迟到配方真正匹配时再解析，失败时视为「永不匹配」。
 */
final class OrigoldsIngredients {
    private static final Identifier MAGIGOLD_CORE_ID =
            Identifier.fromNamespaceAndPath("origolds", "magigold_core");

    private static Ingredient magigoldCore;
    private static boolean magigoldCoreResolved;

    private OrigoldsIngredients() {
    }

    /** 该堆栈是否为前置模组的「魔金之核」；前置模组未提供该物品时永远返回 false。 */
    static boolean isMagigoldCore(ItemStack stack) {
        if (!magigoldCoreResolved) {
            magigoldCore = BuiltInRegistries.ITEM
                    .getOptional(MAGIGOLD_CORE_ID)
                    .map(Ingredient::of)
                    .orElse(null);
            magigoldCoreResolved = true;
        }
        return magigoldCore != null && magigoldCore.test(stack);
    }
}
