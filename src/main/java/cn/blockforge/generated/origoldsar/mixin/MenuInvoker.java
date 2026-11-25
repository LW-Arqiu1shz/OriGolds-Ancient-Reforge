package cn.blockforge.generated.origoldsar.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 调用菜单的受保护方法（给物品栏菜单加行囊格子、Shift 点击进行囊）。 */
@Mixin(AbstractContainerMenu.class)
public interface MenuInvoker {
    @Invoker("addSlot")
    Slot origolds$addSlot(Slot slot);

    @Invoker("moveItemStackTo")
    boolean origolds$moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection);
}
