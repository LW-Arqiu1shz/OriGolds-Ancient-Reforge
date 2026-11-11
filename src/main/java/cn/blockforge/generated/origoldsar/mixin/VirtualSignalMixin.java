package cn.blockforge.generated.origoldsar.mixin;

import cn.blockforge.generated.origoldsar.WirelessRedstoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 把无线红石线路终点与遥控开关的输出注入原版取电路径。
 *
 * <p>1.21.1 中一切红石取电计算（红石粉功率、中继器/比较器输入、活塞、红石灯、
 * 门/活板门等）最终都会调用 {@code BlockState.getSignal(BlockGetter, BlockPos, Direction)}
 * （弱信号）或 {@code BlockState.getDirectSignal(...)}（强信号）。在这两个入口的 RETURN
 * 处查询 {@link WirelessRedstoneManager} 的虚拟电源表：命中则取“原版结果与虚拟强度的
 * 较大者”作为返回值，使被绑定的任意方块表现得像一个红石电源，且信号随线路源头每 tick
 * 联动。
 *
 * <p>只在 RETURN 处“抬高”而不在 HEAD 处“替换”，是因为源方块可能本身就是一个红石电源
 * （拉杆、火把、红石块等）：替换会把它的原版信号整体抹掉，导致“本来该亮反而变暗”。
 * 与此同时，{@link WirelessRedstoneManager#isReadingSource()} 为真时（正在采样线路源）
 * 会跳过注入，避免旁边的虚拟输出回头误供电。
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class VirtualSignalMixin {

    @Inject(method = "getSignal(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I",
            at = @At("RETURN"), cancellable = true)
    private void origoldsAr$virtualWeakSignal(BlockGetter level, BlockPos pos, Direction direction,
                                              CallbackInfoReturnable<Integer> cir) {
        if (!WirelessRedstoneManager.isReadingSource() && level instanceof ServerLevel serverLevel) {
            int signal = WirelessRedstoneManager.peek(serverLevel.dimension(), pos.asLong());
            if (signal > cir.getReturnValue()) {
                cir.setReturnValue(signal);
            }
        }
    }

    @Inject(method = "getDirectSignal(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I",
            at = @At("RETURN"), cancellable = true)
    private void origoldsAr$virtualStrongSignal(BlockGetter level, BlockPos pos, Direction direction,
                                                CallbackInfoReturnable<Integer> cir) {
        if (!WirelessRedstoneManager.isReadingSource() && level instanceof ServerLevel serverLevel) {
            int signal = WirelessRedstoneManager.peek(serverLevel.dimension(), pos.asLong());
            if (signal > cir.getReturnValue()) {
                cir.setReturnValue(signal);
            }
        }
    }
}
