package cn.blockforge.generated.origoldsar.purse;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 放在地上的异界行囊（Shift+右键放下）。大小、碰撞箱和沉重核心一样；右键打开 3×3 界面。
 * 不怕爆炸、活塞推不动、末影龙和凋灵也毁不掉；空手 0.5 秒挖下来，什么工具都不会更快。
 */
public final class OtherworldPurseBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);

    public OtherworldPurseBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties blockProperties() {
        // 硬度 0.33：空手每刻挖 1/(0.33×30)，正好 10 刻（0.5 秒）挖完。抗爆和基岩一样。
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .sound(SoundType.HEAVY_CORE)
                .strength(0.33F, 3600000.0F)
                .pushReaction(PushReaction.IMMOVEABLE)
                .noLootTable();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OtherworldPurseBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof OtherworldPurseBlockEntity purse
                && !purse.purse().isEmpty()) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inventory, p) -> new OtherworldPurseMenu(id, inventory, purse),
                            purse.purse().getHoverName()),
                    buf -> OtherworldPurseMenu.writePlaced(buf, pos));
        }
        return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }
}
