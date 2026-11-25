package cn.blockforge.generated.origoldsar.purse;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 异界行囊：9 格的随身储物。右键打开 3×3 界面，界面里的按钮把它装备到物品栏界面上；Shift+右键方块把它放在地上。
 * 行囊和里面的东西死亡时都不掉落；扔出来的行囊不受重力、不会被烧毁或破坏。
 */
public final class OtherworldPurseItem extends BlockItem {
    public OtherworldPurseItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static boolean isPurse(ItemStack stack) {
        return stack.getItem() instanceof OtherworldPurseItem;
    }

    /** 能不能放进行囊：除了行囊本身，什么都行。 */
    public static boolean canStore(ItemStack stack) {
        return !stack.isEmpty() && !isPurse(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inventory, p) -> new OtherworldPurseMenu(id, inventory, hand, stack),
                            stack.getHoverName()),
                    buf -> OtherworldPurseMenu.writeHand(buf, hand));
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    /** 只有按着 Shift 才放到地上；平时右键（包括对着方块）都是打开界面。 */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    /** 放下时把整个行囊（名字、里面的东西）原样存进方块里。 */
    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (!super.placeBlock(context, state)) {
            return false;
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof OtherworldPurseBlockEntity purse) {
            purse.setPurse(context.getItemInHand().copyWithCount(1));
        }
        return true;
    }

    /** 不能放进潜影盒、收纳袋这类装物品的物品里。 */
    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }
}
