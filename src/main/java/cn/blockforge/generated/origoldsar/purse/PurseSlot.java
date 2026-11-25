package cn.blockforge.generated.origoldsar.purse;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

/** 物品栏菜单里额外加的 9 个行囊格子（只在装备了行囊、并且展开时才显示）。 */
public final class PurseSlot extends Slot {
    /** 菜单坐标里这一行格子的 y（在界面顶边上方）。 */
    public static final int ROW_Y = -20;

    /** 客户端：当前界面是不是会显示这一行的物品栏界面（客户端初始化时替换）。 */
    public static BooleanSupplier clientScreenShowsPanel = () -> false;

    private final PurseContainer purse;

    public PurseSlot(PurseContainer purse, int index) {
        super(purse, index, 8 + index * 18, ROW_Y);
        this.purse = purse;
    }

    private boolean equipped() {
        return purse.data().equipped();
    }

    @Override
    public boolean isActive() {
        if (!equipped()) {
            return false;
        }
        if (purse.owner().level().isClientSide) {
            return purse.data().panelOpen() && clientScreenShowsPanel.getAsBoolean();
        }
        return purse.data().panelOpen();
    }

    // 收起时格子既放不进也拿不出：否则双击物品“收集同类”时会把行囊里的东西也吸出来。
    @Override
    public boolean mayPlace(ItemStack stack) {
        return isActive() && OtherworldPurseItem.canStore(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return isActive();
    }
}
