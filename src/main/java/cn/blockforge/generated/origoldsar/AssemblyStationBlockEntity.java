package cn.blockforge.generated.origoldsar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AssemblyStationBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final String OPTIONS_KEY = "origolds_ar_options";
    private static final String OPTION_DISPLAYS_KEY = "origolds_ar_option_displays";
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final int[] optionTraits = {0, 1, 2};
    private final int[] optionDisplays = {0, 1, 2};

    public AssemblyStationBlockEntity(BlockPos pos, BlockState state) {
        super(GeneratedMod.ORIGOLD_ASSEMBLY_STATION_ENTITY.get(), pos, state);
        reshuffleOptions();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        items.get(0).limitSize(1);
        int[] savedOptions = tag.getIntArray(OPTIONS_KEY);
        if (savedOptions.length == optionTraits.length && isValidOptions(savedOptions)) {
            System.arraycopy(savedOptions, 0, optionTraits, 0, optionTraits.length);
        }
        int[] savedDisplays = tag.getIntArray(OPTION_DISPLAYS_KEY);
        if (savedDisplays.length == optionDisplays.length && isValidDisplayOptions(savedDisplays)) {
            System.arraycopy(savedDisplays, 0, optionDisplays, 0, optionDisplays.length);
        } else {
            reshuffleOptions();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putIntArray(OPTIONS_KEY, optionTraits);
        tag.putIntArray(OPTION_DISPLAYS_KEY, optionDisplays);
        super.saveAdditional(tag, registries);
    }

    public int getOptionTrait(int option) {
        return option >= 0 && option < optionTraits.length ? optionTraits[option] : -1;
    }

    public int getOptionDisplayTrait(int option) {
        if (option < 0 || option >= optionDisplays.length) {
            return -1;
        }
        int[] allocation = TraitLibrary.allocation(optionTraits[option]);
        return allocation[optionDisplays[option]];
    }

    /** Generates a fresh permutation and a fresh display choice for every allocation option. */
    public void reshuffleOptions() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = optionTraits.length - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            int value = optionTraits[index];
            optionTraits[index] = optionTraits[swapIndex];
            optionTraits[swapIndex] = value;
        }
        for (int index = 0; index < optionDisplays.length; index++) {
            optionDisplays[index] = random.nextInt(TraitLibrary.TRAITS_PER_ALLOCATION);
        }
        setChanged();
    }

    public int getOptionSyncValue(int option) {
        if (option < 0 || option >= optionTraits.length) {
            return 0;
        }
        return optionTraits[option] * TraitLibrary.TRAITS_PER_ALLOCATION + optionDisplays[option];
    }

    private static boolean isValidOptions(int[] options) {
        boolean[] seen = new boolean[TraitLibrary.ALLOCATION_COUNT];
        for (int option : options) {
            if (option < 0 || option >= TraitLibrary.ALLOCATION_COUNT || seen[option]) {
                return false;
            }
            seen[option] = true;
        }
        return true;
    }

    private static boolean isValidDisplayOptions(int[] options) {
        for (int option : options) {
            if (option < 0 || option >= TraitLibrary.TRAITS_PER_ALLOCATION) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            stack.limitSize(1);
        } else {
            stack.limitSize(getMaxStackSize());
        }
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.origolds_ar.origold_assembly_station");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AssemblyStationMenu(containerId, inventory, this);
    }
}
