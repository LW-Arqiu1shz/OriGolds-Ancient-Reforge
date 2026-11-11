package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.SelfBottlePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 自循环附魔之瓶的客户端输入：探测 <b>Ctrl + 右键</b>并阻止原版把它当成一次普通的物品使用，
 * 改由 {@link SelfBottlePayload} 上报服务端结算“消耗全部经验并掷出瓶子”。
 *
 * <p>不带 Ctrl 的普通右键不在此拦截，照常走物品自身的 {@code use}（消耗 10 点经验）。
 * Ctrl 属于原版不会同步给服务端的修饰键，因此必须先由客户端探测。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SelfBottleClient {
    private SelfBottleClient() {}

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS
                || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || !isCtrlDown(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.getOverlay() != null) {
            return;
        }
        if (!holdsBottle(minecraft.player) || minecraft.player.totalExperience <= 0) {
            return;
        }
        PacketDistributor.sendToServer(new SelfBottlePayload());
        event.setCanceled(true);
    }

    private static boolean holdsBottle(Player player) {
        return player.getMainHandItem().is(GeneratedMod.SELF_CIRCULATING_BOTTLE.get())
                || player.getOffhandItem().is(GeneratedMod.SELF_CIRCULATING_BOTTLE.get());
    }

    /** Ctrl 判定：优先用鼠标事件自带的修饰位，再用实时按键状态兜底。 */
    private static boolean isCtrlDown(InputEvent.MouseButton.Pre event) {
        if ((event.getModifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            return true;
        }
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
}
