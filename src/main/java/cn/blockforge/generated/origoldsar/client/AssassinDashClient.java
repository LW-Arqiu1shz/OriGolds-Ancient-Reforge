package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.AssassinDashPayload;
import cn.blockforge.generated.origoldsar.AssassinLeapPayload;
import cn.blockforge.generated.origoldsar.AbilityItems;
import cn.blockforge.generated.origoldsar.GeneratedMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Detects the assassin-medallion dash input on the client: while the dash key
 * (default SPACE) is held down, double-tapping a movement direction key sends
 * a dash request to the server for that direction.
 *
 * 按键状态改为直接轮询 GLFW 的真实物理状态（InputConstants.isKeyDown），
 * 而不是 KeyMapping.isDown()：KeyMapping 的内部状态会被打开界面、失焦等
 * 情况下的 releaseAll/事件丢失弄脏（卡在 true 或 false），一旦脱钩，
 * 双击的“按下沿”就永远检测不到，突进表现为“有时没反应”。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class AssassinDashClient {
    /** Max ticks between the two presses to count as a double tap (0.6s). */
    private static final int DOUBLE_PRESS_TICKS = 12;
    private static final int COOLDOWN_TICKS = 60;

    private static final KeyMapping DASH = OrigoldKeyBindings.DASH;

    private static int clientTick;
    // 水平冲刺与大跳各自独立的冷却计时
    private static int cooldownUntil;
    private static int leapCooldownUntil;
    // index order matches the payload: 0=W, 1=A, 2=S, 3=D
    private static boolean[] prevDown = {false, false, false, false};
    private static int[] lastTap = {-9999, -9999, -9999, -9999};
    // Tab 键（大跳）的边缘检测状态，独立于方向键数组
    private static boolean prevTabDown;
    private static int lastTabTap = -9999;

    private AssassinDashClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            clientTick++;
            prevDown[0] = prevDown[1] = prevDown[2] = prevDown[3] = false;
            prevTabDown = false;
            return;
        }
        clientTick++;

        // 打开任何界面（聊天、背包等）时不响应突进，且清一次边缘状态，
        // 避免界面吞掉按键事件后产生假边缘。
        if (mc.screen != null) {
            prevDown[0] = prevDown[1] = prevDown[2] = prevDown[3] = false;
            prevTabDown = false;
            return;
        }

        boolean dashHeld = isPhysicallyDown(DASH);

        KeyMapping[] dirs = {mc.options.keyUp, mc.options.keyLeft, mc.options.keyDown, mc.options.keyRight};
        for (int i = 0; i < 4; i++) {
            boolean down = isPhysicallyDown(dirs[i]);
            boolean edge = down && !prevDown[i];
            prevDown[i] = down;

            // The double tap must happen while the dash key is held, so any
            // taps made before pressing it are discarded.
            if (!dashHeld) {
                lastTap[i] = -9999;
                continue;
            }
            if (edge) {
                if (clientTick - lastTap[i] <= DOUBLE_PRESS_TICKS && canDash(player)) {
                    triggerDash((byte) i);
                    lastTap[i] = -9999;
                } else {
                    lastTap[i] = clientTick;
                }
            }
        }

        // 双击 Tab（按住快捷键时）触发向上大跳，冷却与水平冲刺相互独立。
        boolean tabDown = isTabDown(mc);
        boolean tabEdge = tabDown && !prevTabDown;
        prevTabDown = tabDown;

        if (!dashHeld) {
            lastTabTap = -9999;
        } else if (tabEdge) {
            if (clientTick - lastTabTap <= DOUBLE_PRESS_TICKS
                    && clientTick >= leapCooldownUntil
                    && hasMedallion(player)) {
                leapCooldownUntil = clientTick + COOLDOWN_TICKS;
                ClientPacketDistributor.sendToServer(new AssassinLeapPayload());
                lastTabTap = -9999;
            } else {
                lastTabTap = clientTick;
            }
        }
    }

    /** Tab 是原版玩家列表键，直接查 GLFW 物理状态，避免与原版 KeyMapping 互相干扰。 */
    private static boolean isTabDown(Minecraft mc) {
        return GLFW.glfwGetKey(mc.getWindow().handle(), GLFW.GLFW_KEY_TAB) == GLFW.GLFW_PRESS;
    }

    /**
     * 读取按键的物理状态；鼠标/其它类型按键退回 KeyMapping 状态。
     * 直接查 GLFW，不依赖会被界面与失焦弄脏的 KeyMapping 内部状态。
     */
    private static boolean isPhysicallyDown(KeyMapping mapping) {
        InputConstants.Key key = mapping.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()) {
            long window = Minecraft.getInstance().getWindow().handle();
            return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return mapping.isDown();
    }

    private static boolean canDash(LocalPlayer player) {
        return clientTick >= cooldownUntil && hasMedallion(player);
    }

    private static void triggerDash(byte direction) {
        cooldownUntil = clientTick + COOLDOWN_TICKS;
        ClientPacketDistributor.sendToServer(new AssassinDashPayload(direction));
    }

    /** “持有时”按物品栏整体判定（主手、副手或背包中任意一格）。 */
    private static boolean hasMedallion(LocalPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (AbilityItems.hasMedallionAbility(stack, GeneratedMod.ASSASSIN_MEDALLION.get())) {
                return true;
            }
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return AbilityItems.hasMedallionAbility(main, GeneratedMod.ASSASSIN_MEDALLION.get())
                || AbilityItems.hasMedallionAbility(off, GeneratedMod.ASSASSIN_MEDALLION.get());
    }
}
