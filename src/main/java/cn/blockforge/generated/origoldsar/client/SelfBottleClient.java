package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.SelfBottlePayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 自循环附魔之瓶的客户端快捷键（1.2.0 新功能）：
 * 手持瓶子时按住 Ctrl 点右键，把身上全部经验（单瓶上限 32767）装瓶掷出。
 * 本地只做"值不值得发请求"的粗筛（持瓶、有经验、没开界面），
 * 真正的扣经验与生成实体都由服务端复核后执行。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class SelfBottleClient {
    private SelfBottleClient() {}

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT
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
        ClientPacketDistributor.sendToServer(new SelfBottlePayload());
        event.setCanceled(true);
    }

    private static boolean holdsBottle(Player player) {
        return player.getMainHandItem().is(GeneratedMod.SELF_CIRCULATING_BOTTLE.get())
                || player.getOffhandItem().is(GeneratedMod.SELF_CIRCULATING_BOTTLE.get());
    }

    /** Ctrl 判定：优先用鼠标事件自带的 GLFW 修饰位，再用实时按键状态兜底。 */
    private static boolean isCtrlDown(InputEvent.MouseButton.Pre event) {
        if ((event.getModifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            return true;
        }
        // 26.1.2：InputConstants.isKeyDown 直接接收 Window。
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
}
