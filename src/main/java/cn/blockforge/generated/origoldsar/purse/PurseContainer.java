package cn.blockforge.generated.origoldsar.purse;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 物品栏界面上方那一行格子背后的容器：每次都现取玩家的 {@link PurseData}，
 * 所以玩家重生、附件被复制之后也不会指向旧数据。
 */
public final class PurseContainer implements Container {
    private final Player owner;

    public PurseContainer(Player owner) {
        this.owner = owner;
    }

    public Player owner() {
        return owner;
    }

    public PurseData data() {
        return owner.getData(GeneratedMod.PURSE_DATA);
    }

    @Override
    public int getContainerSize() {
        return PurseData.SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : data().items()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return data().items().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(data().items(), slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(data().items(), slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        data().items().set(slot, stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        data().items().replaceAll(s -> ItemStack.EMPTY);
    }
}
