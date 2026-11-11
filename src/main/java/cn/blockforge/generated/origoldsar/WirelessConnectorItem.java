package cn.blockforge.generated.origoldsar;

import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 金源无线红石连接装置：潜行 + 左键记录起点坐标、潜行 + 右键记录终点坐标
 * （两个坐标记录点，先后顺序不限，同侧再次点击覆盖旧值；只标了一个点时，
 * 再对同一方块按同一条边的键可取消该标记），两点齐后自动在它们
 * 之间建立一条无线红石线路——终点会像电源一样输出起点的信号强度。
 * 潜行 + 左键 / 右键走 32 格射线选点（见
 * {@link cn.blockforge.generated.origoldsar.client.WirelessConnectorClient}）；
 * 普通左键/右键不受影响（照常挖掘、照常使用被点击的方块）。
 *
 * <p>手持时潜行 + 中键切换“无线红石显示模式”（渲染在客户端
 * {@link cn.blockforge.generated.origoldsar.client.WirelessConnectorClient}）：
 * 显示模式下渲染穿墙的“输入→输出”渐变粒子线；准星悬停某条线路时
 * 端点描边变白，此时按 Delete 删除、按 Backspace 反转该线路方向（统一 32 格射程）。
 * 模式状态（所处模式 + 开/关）跨游戏会话记忆，见
 * {@link cn.blockforge.generated.origoldsar.client.DisplayModeMemory}。
 *
 * <p>手持时准星命中的方块会用白色描边高亮（只描边，统一 32 格射程），
 * 方便知道自己正在选哪个方块。
 *
 * <p><b>Ctrl+Z / Ctrl+Y</b>：撤销 / 重做本玩家的无线线路操作（新增 / 删除连线、
 * 反转输入输出方向）。手持任意连接装置通用，最多 16 条，玩家退出世界即清除。
 *
 * <p>实用提示（浅蓝色）与风味引言的悬浮提示在客户端
 * {@link cn.blockforge.generated.origoldsar.client.ClientGameEvents} 里挂到
 * ItemTooltipEvent 上绘制：长按 Shift 可展开除首句外的全部提示明细。
 */
public final class WirelessConnectorItem extends Item {
    public WirelessConnectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 仅潜行 + 右键用于记录终点坐标点；普通右键放行给被点的方块（开箱、按按钮等原版行为）。
        // 服务端用 isCrouching()（潜行状态经 START_SNEAKING 指令同步，比按键状态字段可靠）。
        if (context.getPlayer() == null || !context.getPlayer().isCrouching()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
            WirelessRedstoneManager.onUseOn(player, context.getClickedPos());
        }
        return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }
}
