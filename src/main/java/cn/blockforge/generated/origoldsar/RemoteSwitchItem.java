package cn.blockforge.generated.origoldsar;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 金源无线红石遥控开关：潜行 + 左键点击一个方块与之绑定（绑定信息存在物品组件里，
 * 普通左键照常挖掘）。
 *
 * <p>右键使用：
 * <ul>
 *   <li>绑定的是拉杆、按钮、雷石灯柱等可被玩家直接点击交互的红石方块时，远程“按一次”；</li>
 *   <li>否则像远程拉杆一样，切换绑定方块位置的 15 级红石信号输出（开着时再用一次即关闭）。</li>
 * </ul>
 *
 * <p>手持时潜行 + 中键切换遥控显示模式：绑定的方块发出可穿墙的淡蓝色光
 * （渲染在客户端 {@link cn.blockforge.generated.origoldsar.client.WirelessConnectorClient}）。
 *
 * <p>绑定走统一 32 格射程，且必须命中真实方块（不能绑定到空气）。手持时准星命中的方块
 * 用白色描边高亮；对<b>已绑定的方块</b>的悬停选中采用与线路悬停同源的穿墙几何检测
 * （无视中间遮挡，32 格内），选中的方块直接变成白色，此时按 <b>Delete</b> 可解除绑定。
 *
 * <p><b>Ctrl+Z / Ctrl+Y</b>：撤销 / 重做<b>这一把</b>遥控开关自己的绑定 / 解绑操作
 * （每一把开关独立保存最多 16 条，玩家退出世界即清除）。
 */
public final class RemoteSwitchItem extends Item {
    public RemoteSwitchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
            trigger(player, context.getHand());
        }
        return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }

    @Override
    public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            trigger(serverPlayer, hand);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    /** 远程触发一次，带短暂使用冷却，防止连点反复开关信号。 */
    private void trigger(ServerPlayer player, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(player.getItemInHand(hand))) {
            return;
        }
        player.getCooldowns().addCooldown(player.getItemInHand(hand), 8);
        WirelessRedstoneManager.onTrigger(player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.origolds_ar.remote_switch"));
        tooltip.accept(Component.translatable("tooltip.origolds_ar.remote_switch_quote")
                .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));
        if (stack.get(GeneratedMod.REMOTE_SWITCH_BINDING.get()) instanceof RemoteSwitchBinding binding) {
            tooltip.accept(Component.translatable("tooltip.origolds_ar.remote_switch_bound",
                    binding.position().getX(), binding.position().getY(), binding.position().getZ()));
            tooltip.accept(Component.translatable(binding.active()
                    ? "tooltip.origolds_ar.remote_switch_on"
                    : "tooltip.origolds_ar.remote_switch_off"));
        } else {
            tooltip.accept(Component.translatable("tooltip.origolds_ar.remote_switch_unbound"));
        }
    }
}
