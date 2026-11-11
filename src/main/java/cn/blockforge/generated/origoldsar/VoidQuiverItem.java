package cn.blockforge.generated.origoldsar;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;

/**
 * 虚空箭匣：封装了一种箭矢（包括药水箭、光灵箭等）的“永不被消耗”版本。
 * 匣内箭矢堆栈保存在 {@code origolds_ar:quiver_arrow} 组件里，物品名自动跟随内容
 * （虚空[箭矢名称]箭匣）。玩家背包中没有真正的箭矢时，弓/弩会直接从箭匣取箭射击
 * （见 {@link VoidQuiverEvents}），箭匣与匣内箭矢都不会被消耗。
 */
public final class VoidQuiverItem extends Item {
    public VoidQuiverItem(Properties properties) {
        super(properties);
    }

    /** 用任意箭矢堆栈创建一个装填了该箭的虚空箭匣。 */
    public static ItemStack create(ItemStack arrow) {
        ItemStack result = new ItemStack(GeneratedMod.VOID_QUIVER.get());
        result.set(GeneratedMod.QUIVER_ARROW.get(), arrow.copyWithCount(1));
        return result;
    }

    /** 是否为可作为箭匣内容的箭矢（普通箭、药水箭、光灵箭等，走原版 #minecraft:arrows 标签）。 */
    public static boolean isArrow(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.ARROWS);
    }

    /** 是否为烟花火箭（1.2.0 起箭匣也可封装烟花，供弩发射且不消耗）。 */
    public static boolean isFireworkRocket(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.FIREWORK_ROCKET);
    }

    /** 是否为可作为箭匣内容物的投射物：箭矢或烟花火箭。 */
    public static boolean isProjectile(ItemStack stack) {
        return isArrow(stack) || isFireworkRocket(stack);
    }

    /** 箭匣中封装的箭矢堆栈（可能为 EMPTY）。 */
    public static ItemStack arrowOf(ItemStack quiver) {
        ItemStack arrow = quiver.get(GeneratedMod.QUIVER_ARROW.get());
        return arrow == null ? ItemStack.EMPTY : arrow;
    }

    @Override
    public ItemStack getDefaultInstance() {
        return create(new ItemStack(Items.ARROW));
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack arrow = arrowOf(stack);
        if (arrow.isEmpty()) {
            return Component.translatable(this.getDescriptionId());
        }
        return Component.translatable("text.origolds_ar.void_quiver_named", arrow.getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ItemStack arrow = arrowOf(stack);
        if (!arrow.isEmpty()) {
            arrow.getItem().appendHoverText(arrow, context, display, tooltipComponents, tooltipFlag);
        }
    }
}
