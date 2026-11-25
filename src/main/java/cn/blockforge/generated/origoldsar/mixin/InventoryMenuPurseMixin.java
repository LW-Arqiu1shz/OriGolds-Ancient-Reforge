package cn.blockforge.generated.origoldsar.mixin;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.purse.OtherworldPurseItem;
import cn.blockforge.generated.origoldsar.purse.PurseContainer;
import cn.blockforge.generated.origoldsar.purse.PurseData;
import cn.blockforge.generated.origoldsar.purse.PurseSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 给玩家物品栏菜单加 9 个异界行囊格子（排在原有格子之后），
 * 并让行囊展开时 Shift 点击背包里的物品优先放进行囊。
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuPurseMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void origolds$addPurseSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        PurseContainer container = new PurseContainer(owner);
        MenuInvoker menu = (MenuInvoker) this;
        for (int i = 0; i < PurseData.SIZE; i++) {
            menu.origolds$addSlot(new PurseSlot(container, i));
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void origolds$quickMoveIntoPurse(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        PurseData data = player.getData(GeneratedMod.PURSE_DATA);
        if (!data.panelOpen()) {
            return;
        }
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (index < 0 || index >= self.slots.size()) {
            return;
        }
        Slot slot = self.slots.get(index);
        // 只处理背包和快捷栏（不含盔甲和副手）。
        if (!(slot.container instanceof Inventory) || slot.getContainerSlot() >= 36) {
            return;
        }
        ItemStack stack = slot.getItem();
        if (!OtherworldPurseItem.canStore(stack)) {
            return;
        }
        int start = -1;
        int end = -1;
        for (Slot candidate : self.slots) {
            if (candidate instanceof PurseSlot) {
                if (start < 0) {
                    start = candidate.index;
                }
                end = candidate.index + 1;
            }
        }
        if (start < 0 || !((MenuInvoker) this).origolds$moveItemStackTo(stack, start, end, false)) {
            return;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        cir.setReturnValue(ItemStack.EMPTY);
    }
}
