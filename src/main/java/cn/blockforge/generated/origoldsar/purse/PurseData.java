package cn.blockforge.generated.origoldsar.purse;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家身上的异界行囊数据（NeoForge 附件，死亡后复制给新玩家）。
 *
 * <p>装备时行囊本身存在 {@link #purse}，里面的 9 格物品拆出来放在 {@link #items}
 * （物品栏界面上方那一行格子直接读写它）；取下时再把物品写回行囊的 {@code container} 组件。
 * {@link #kept} 是死亡时从物品栏里收走、等重生后还给玩家的行囊。
 */
public final class PurseData implements INBTSerializable<CompoundTag> {
    public static final int SIZE = 9;

    private ItemStack purse = ItemStack.EMPTY;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final List<ItemStack> kept = new ArrayList<>();
    /** 物品栏界面上方那一行是否展开（不存档；Shift 点击时用它决定物品去向）。 */
    private boolean panelOpen;

    public boolean equipped() {
        return !purse.isEmpty();
    }

    /**
     * 装备着的行囊里的物品。行囊算作物品栏的延伸：本模组的勋章、符文、复苏图腾、虚空箭袋放在里面也生效。
     * 没装备时为空。客户端的格子内容由物品栏菜单同步过来，所以两边都能用。
     */
    public static List<ItemStack> contents(net.minecraft.world.entity.player.Player player) {
        PurseData data = player.getData(cn.blockforge.generated.origoldsar.GeneratedMod.PURSE_DATA);
        return data.equipped() ? data.items : List.of();
    }

    /** 背包主体（含快捷栏）加上装备着的行囊。 */
    public static Iterable<ItemStack> withInventory(net.minecraft.world.entity.player.Player player) {
        return com.google.common.collect.Iterables.concat(player.getInventory().items, contents(player));
    }

    public ItemStack purse() {
        return purse;
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public List<ItemStack> kept() {
        return kept;
    }

    public boolean panelOpen() {
        return panelOpen && equipped();
    }

    public void setPanelOpen(boolean open) {
        panelOpen = open;
    }

    /** 装备一个行囊：行囊里的物品搬进 {@link #items}。调用前应先 {@link #unequip()}。 */
    public void equip(ItemStack stack) {
        purse = stack.copyWithCount(1);
        items.replaceAll(s -> ItemStack.EMPTY);
        purse.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        purse.remove(DataComponents.CONTAINER);
    }

    /** 取下行囊：物品写回行囊并返回它；没装备时返回空。 */
    public ItemStack unequip() {
        if (!equipped()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = purse;
        out.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        purse = ItemStack.EMPTY;
        items.replaceAll(s -> ItemStack.EMPTY);
        panelOpen = false;
        return out;
    }

    /** 客户端：收到同步包时只换行囊本身（格子内容由物品栏菜单自己同步）。 */
    public void setClientPurse(ItemStack stack, boolean open) {
        purse = stack;
        panelOpen = open;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (equipped()) {
            tag.put("Purse", purse.save(provider));
        }
        ContainerHelper.saveAllItems(tag, items, provider);
        if (!kept.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : kept) {
                list.add(stack.save(provider));
            }
            tag.put("Kept", list);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        purse = tag.contains("Purse", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(provider, tag.getCompound("Purse"))
                : ItemStack.EMPTY;
        items.replaceAll(s -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        kept.clear();
        ListTag list = tag.getList("Kept", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(provider, list.getCompound(i));
            if (!stack.isEmpty()) {
                kept.add(stack);
            }
        }
    }
}
