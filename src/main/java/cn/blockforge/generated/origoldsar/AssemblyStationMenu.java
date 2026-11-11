package cn.blockforge.generated.origoldsar;

import net.minecraft.util.Prediction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AssemblyStationMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int TRAIT_BUTTONS = TraitLibrary.ALLOCATION_COUNT;
    private static final int PLAYER_SLOT_START = 1;
    private static final int PLAYER_SLOT_END = 37;

    private final AssemblyStationBlockEntity station;
    private final boolean clientMenu;
    private final int[] syncedOptionTraits = {0, 1, 2};

    public AssemblyStationMenu(int containerId, Inventory inventory, AssemblyStationBlockEntity station) {
        this(containerId, inventory, station, false);
    }

    private AssemblyStationMenu(int containerId, Inventory inventory, AssemblyStationBlockEntity station, boolean clientMenu) {
        super(GeneratedMod.ASSEMBLY_STATION_MENU.get(), containerId);
        this.station = station;
        this.clientMenu = clientMenu;
        addSlot(new InputSlot(station, 0, 46, 56));
        addPlayerInventory(inventory);
        addDataSlots(new OptionData());
    }

    public AssemblyStationMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, findStation(inventory, buffer.readBlockPos()), true);
    }

    private static AssemblyStationBlockEntity findStation(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof AssemblyStationBlockEntity station) {
            return station;
        }
        return new AssemblyStationBlockEntity(pos, GeneratedMod.ORIGOLD_ASSEMBLY_STATION.get().defaultBlockState());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 19 + column * 18, 132 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 19 + column * 18, 186));
        }
    }

    public int getCandidate(int index) {
        if (index < 0 || index >= TRAIT_BUTTONS) {
            return -1;
        }
        int syncValue = clientMenu ? syncedOptionTraits[index] : station.getOptionSyncValue(index);
        int allocation = syncValue / TraitLibrary.TRAITS_PER_ALLOCATION;
        int displayIndex = syncValue % TraitLibrary.TRAITS_PER_ALLOCATION;
        return TraitLibrary.allocation(allocation)[displayIndex];
    }

    public boolean hasInput() {
        ItemStack input = station.getItem(INPUT_SLOT);
        return input.is(GeneratedMod.BROKEN_ORIGOLD_PARTS.get()) && !TraitLibrary.isInfused(input);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= TRAIT_BUTTONS || !hasInput()) {
            return false;
        }
        int selectedTrait = station.getOptionDisplayTrait(id);
        ItemStack infused = station.getItem(INPUT_SLOT).copyWithCount(1);
        TraitLibrary.writeSelectedTraits(infused, TraitLibrary.randomAllocationContaining(selectedTrait));
        station.setItem(INPUT_SLOT, infused);
        station.reshuffleOptions();
        broadcastChanges();
        if (player instanceof ServerPlayer serverPlayer) {
            GeneratedMod.ORIGOLD_INFUSION_TRIGGER.get().trigger(serverPlayer);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem().copy();
        if (index == INPUT_SLOT) {
            if (!moveItemStackTo(slot.getItem(), PLAYER_SLOT_START, PLAYER_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_SLOT_START && index < PLAYER_SLOT_END) {
            if (!moveItemStackTo(slot.getItem(), INPUT_SLOT, INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (slot.getItem().isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, original);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return station.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            player.getInventory().placeItemBackInInventory(station.removeItemNoUpdate(INPUT_SLOT), Prediction.SERVER_ONLY);
        }
    }

    private final class OptionData implements ContainerData {
        @Override
        public int get(int index) {
            return station.getOptionSyncValue(index);
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < TRAIT_BUTTONS
                    && value >= 0
                    && value < TraitLibrary.ALLOCATION_COUNT * TraitLibrary.TRAITS_PER_ALLOCATION) {
                syncedOptionTraits[index] = value;
            }
        }

        @Override
        public int getCount() {
            return TRAIT_BUTTONS;
        }
    }

    private static final class InputSlot extends Slot {
        private InputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(GeneratedMod.BROKEN_ORIGOLD_PARTS.get()) && !TraitLibrary.isInfused(stack);
        }
    }
}
