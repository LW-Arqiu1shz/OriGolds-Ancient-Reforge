package cn.blockforge.generated.origoldsar.purse;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** 异界行囊的 3×3 界面：手持右键打开，或者右键放在地上的行囊打开。按钮 0 = 装备（只有手持打开时有）。 */
public final class OtherworldPurseMenu extends AbstractContainerMenu {
    public static final int BUTTON_EQUIP = 0;
    private static final int PURSE_SLOTS = PurseData.SIZE;
    private static final int INV_END = PURSE_SLOTS + 36;

    /** 手持打开时是哪只手；从地上的行囊打开时为 null。 */
    private final InteractionHand hand;
    /** 服务端：被打开的那个行囊；客户端为空。 */
    private final ItemStack stack;
    private final Container container;
    /** 服务端、从地上的行囊打开时：那个方块。 */
    private final OtherworldPurseBlockEntity block;
    private final boolean placed;
    /** 行囊所在的快捷栏格子（副手时为 -1），这一格锁住不让动。 */
    private final int lockedHotbar;

    public static void writeHand(FriendlyByteBuf buf, InteractionHand hand) {
        buf.writeBoolean(false);
        buf.writeEnum(hand);
    }

    public static void writePlaced(FriendlyByteBuf buf, BlockPos pos) {
        buf.writeBoolean(true);
        buf.writeBlockPos(pos);
    }

    /** 客户端构造。 */
    public static OtherworldPurseMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            buf.readBlockPos();
            return new OtherworldPurseMenu(id, inventory, null, ItemStack.EMPTY, new SimpleContainer(PURSE_SLOTS), null, true);
        }
        return new OtherworldPurseMenu(id, inventory, buf.readEnum(InteractionHand.class),
                ItemStack.EMPTY, new SimpleContainer(PURSE_SLOTS), null, false);
    }

    /** 服务端构造：手持打开。 */
    public OtherworldPurseMenu(int id, Inventory inventory, InteractionHand hand, ItemStack stack) {
        this(id, inventory, hand, stack, new StackContainer(stack, () -> { }), null, false);
    }

    /** 服务端构造：右键地上的行囊打开。 */
    public OtherworldPurseMenu(int id, Inventory inventory, OtherworldPurseBlockEntity block) {
        this(id, inventory, null, block.purse(), block.container(), block, true);
    }

    private OtherworldPurseMenu(int id, Inventory inventory, InteractionHand hand, ItemStack stack, Container container,
                                OtherworldPurseBlockEntity block, boolean placed) {
        super(GeneratedMod.OTHERWORLD_PURSE_MENU.get(), id);
        this.hand = hand;
        this.stack = stack;
        this.container = container;
        this.block = block;
        this.placed = placed;
        this.lockedHotbar = hand == InteractionHand.MAIN_HAND ? inventory.selected : -1;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(container, column + row * 3, 62 + column * 18, 17 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack item) {
                        return OtherworldPurseItem.canStore(item);
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            if (column == lockedHotbar) {
                addSlot(new Slot(inventory, column, 8 + column * 18, 142) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(ItemStack item) {
                        return false;
                    }
                });
            } else {
                addSlot(new Slot(inventory, column, 8 + column * 18, 142));
            }
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // 数字键 / F 键也不能把正打开的行囊换走。
        if (clickType == ClickType.SWAP && (button == lockedHotbar
                || (hand == InteractionHand.OFF_HAND && button == Inventory.SLOT_OFFHAND))) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_EQUIP || hand == null || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held != stack || !OtherworldPurseItem.isPurse(held)) {
            return false;
        }
        PurseData data = player.getData(GeneratedMod.PURSE_DATA);
        ItemStack previous = data.unequip();
        data.equip(held);
        player.setItemInHand(hand, previous);
        PurseNetwork.sync(serverPlayer);
        serverPlayer.closeContainer();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack item = slot.getItem();
        ItemStack original = item.copy();
        if (index < PURSE_SLOTS) {
            if (!moveItemStackTo(item, PURSE_SLOTS, INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(item, 0, PURSE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (item.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** 是不是从地上的行囊打开的（这时没有「装备」按钮）。 */
    public boolean isPlaced() {
        return placed;
    }

    @Override
    public boolean stillValid(Player player) {
        if (block != null) {
            return !block.isRemoved() && block.purse() == stack && Container.stillValidBlockEntity(block, player);
        }
        return stack.isEmpty() || player.getItemInHand(hand) == stack;
    }

    /** 服务端：直接读写行囊物品上的 container 组件，每次改动都写回。 */
    static final class StackContainer extends SimpleContainer {
        private final ItemStack stack;
        private final Runnable onChange;

        StackContainer(ItemStack stack, Runnable onChange) {
            super(PURSE_SLOTS);
            this.stack = stack;
            this.onChange = onChange;
            stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(getItems());
        }

        @Override
        public void setChanged() {
            super.setChanged();
            stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
            onChange.run();
        }
    }
}
