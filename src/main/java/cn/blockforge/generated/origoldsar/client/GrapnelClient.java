package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.GrapnelActionPayload;
import cn.blockforge.generated.origoldsar.GrapnelPreviewTogglePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 钩爪弩炮的客户端输入：手持该弩时把左键 / 中键单击交给服务端处理。
 *
 * <p>用 {@link InputEvent.MouseButton.Pre} 而不是轮询 {@code options.keyAttack}：
 * 后者会被原版“挥空”造成的 missTime 吃掉连续点击，也会在蓄力（右键按住）时被丢弃；
 * 鼠标回调只关心“按下的那一下”，并且不拦截蓄力与发射本身。
 *
 * <p>此外还负责“路径预览”：Ctrl + 中键切换开关，开启时沿着“玩家 -> 当前钩爪目标”
 * 的直线撒粒子，直观展示使用钩爪后会飞向的位置。目标位置由服务端每 tick 流式同步过来。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class GrapnelClient {

    /** 是否开启了路径预览（Ctrl + 中键切换，客户端本地状态）。 */
    private static boolean pathPreviewEnabled = false;
    /** 服务端流式同步来的当前钩爪目标坐标，没有目标时为 {@code null}。 */
    private static Vec3 previewTarget = null;

    private GrapnelClient() {}

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        // 界面的点击交给界面处理。
        if (minecraft.screen != null || minecraft.getOverlay() != null) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || !holdsBallista(player)) {
            return;
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // 左键：拉向箭矢/生物，潜行时改为拉生物。潜行状态在服务端再读一次为准。
            ClientPacketDistributor.sendToServer(new GrapnelActionPayload(GrapnelActionPayload.ACTION_LEFT_CLICK));
            event.setCanceled(true);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            if (player.isShiftKeyDown()) {
                // 潜行 + 中键：清除绑定并中断拖拽（不潜行时保留原版选取方块）。
                ClientPacketDistributor.sendToServer(new GrapnelActionPayload(GrapnelActionPayload.ACTION_CLEAR));
                event.setCanceled(true);
            } else if (isControlDown()) {
                // Ctrl + 中键：切换钩爪路径预览开关。
                togglePreview(player);
                event.setCanceled(true);
            }
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && player.isShiftKeyDown()) {
            // 潜行 + 右键：立刻取消当前动作（正在被拉过去时“下车”），但保留与箭矢的绑定。
            // 拦截潜行时的右键按下，避免触发弩的蓄力/发射。
            ClientPacketDistributor.sendToServer(new GrapnelActionPayload(GrapnelActionPayload.ACTION_DROP));
            event.setCanceled(true);
        }
    }

    private static void togglePreview(LocalPlayer player) {
        pathPreviewEnabled = !pathPreviewEnabled;
        if (!pathPreviewEnabled) {
            previewTarget = null;
        }
        // 让服务端知道要开始/停止流式同步钩爪目标位置。
        ClientPacketDistributor.sendToServer(new GrapnelPreviewTogglePayload(pathPreviewEnabled));
    }

    /** 服务端流式同步来的钩爪目标位置（由服务端包处理器调用）。 */
    public static void onPreviewSync(boolean hasTarget, double x, double y, double z) {
        if (hasTarget) {
            previewTarget = new Vec3(x, y, z);
        } else {
            previewTarget = null;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        if (!pathPreviewEnabled || !holdsBallista(player)) {
            return;
        }
        Vec3 target = previewTarget;
        if (target == null) {
            return;
        }
        // 从玩家的脚（实体所在位置）连到钩爪目标，而不是眼睛。
        Vec3 start = player.position();
        double distance = start.distanceTo(target);
        if (distance < 0.1D) {
            return;
        }
        // 沿着“玩家脚部 -> 目标”的线段每 0.6 格撒一颗粒子，形成可见的路径线。
        // 每颗粒子 setLifetime(1)：年龄在下一 tick 到达 lifetime 立即被移除，
        // 所以只渲染当前这一帧，旧粒子不会残留，移动时也就没有拖尾。
        ParticleEngine particleEngine = minecraft.particleEngine;
        int count = Math.min(40, Math.max(2, (int) Math.ceil(distance / 0.6D)));
        for (int i = 0; i < count; i++) {
            double t = (double) i / (count - 1);
            Vec3 p = start.lerp(target, t);
            Particle particle = particleEngine.createParticle(
                    ParticleTypes.END_ROD, p.x, p.y, p.z, 0.0D, 0.01D, 0.0D);
            if (particle != null) {
                particle.setLifetime(1);
            }
        }
    }

    private static boolean isControlDown() {
        // 26.1.2：InputConstants.isKeyDown 直接接收 Window（GLFW 句柄为 Window#handle()，此处用不到）。
        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL);
    }

    private static boolean holdsBallista(LocalPlayer player) {
        return player.getMainHandItem().is(GeneratedMod.GRAPNEL_BALLISTA.get())
                || player.getOffhandItem().is(GeneratedMod.GRAPNEL_BALLISTA.get());
    }
}
