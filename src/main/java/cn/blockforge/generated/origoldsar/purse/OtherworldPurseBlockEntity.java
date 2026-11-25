package cn.blockforge.generated.origoldsar.purse;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 放在地上的异界行囊：整个行囊物品（连同名字、里面的东西）原样存着，挖掉时原样还回来。 */
public final class OtherworldPurseBlockEntity extends BlockEntity {
    private static final String PURSE_KEY = "purse";

    private ItemStack purse = ItemStack.EMPTY;
    /** 几个人同时打开时共用这一个容器，不会各改各的。 */
    private Container container;

    public OtherworldPurseBlockEntity(BlockPos pos, BlockState state) {
        super(GeneratedMod.OTHERWORLD_PURSE_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStack purse() {
        return purse;
    }

    public void setPurse(ItemStack stack) {
        purse = stack;
        container = null;
        setChanged();
    }

    public Container container() {
        if (container == null) {
            container = new OtherworldPurseMenu.StackContainer(purse, this::setChanged);
        }
        return container;
    }

    /** 把行囊掉在方块中间（不看「方块掉落」游戏规则，免得行囊和里面的东西就这么没了）。 */
    void dropPurse() {
        if (level == null || level.isClientSide() || purse.isEmpty()) {
            return;
        }
        // 掉出去的就是同一个物品：界面还开着的人在关掉之前改的东西也都跟着它走，不会多出来一份。
        ItemEntity item = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.25,
                worldPosition.getZ() + 0.5, purse, 0.0, 0.0, 0.0);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        purse = ItemStack.EMPTY;
        container = null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!purse.isEmpty()) {
            output.store(PURSE_KEY, ItemStack.CODEC, purse);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        purse = input.read(PURSE_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        container = null;
    }

    /** 方块不管怎么没的（挖掉、/setblock……），都把行囊连同里面的东西原样掉出来。 */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropPurse();
    }
}
