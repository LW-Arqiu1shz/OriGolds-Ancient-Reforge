package cn.blockforge.generated.origoldsar.api.visual;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/**
 * 一帧无线视觉绘制的上下文：相机、玩家、当前维度与本帧画布。
 *
 * <p>由 {@link WireVisuals} 在每帧创建并交给每个 {@link WireVisualSource}，
 * 附属模组在 {@code collect} 里通过 {@link #canvas()} 画自己的线路与端点，
 * 通过 {@link #dimension()} 过滤维度、通过 {@link #player()} 判断"玩家手上拿的是不是我的装置"。
 */
public final class WireVisualContext {
    private final ClientLevel level;
    private final ResourceKey<Level> dimension;
    private final LocalPlayer player;
    private final Vec3 camera;
    private final Vec3 look;
    private final WireVisualCanvas canvas;

    WireVisualContext(ClientLevel level, ResourceKey<Level> dimension, LocalPlayer player,
                      Vec3 camera, Vec3 look, WireVisualCanvas canvas) {
        this.level = level;
        this.dimension = dimension;
        this.player = player;
        this.camera = camera;
        this.look = look;
        this.canvas = canvas;
    }

    /** 本帧画布（绝对世界坐标，模块内部会换算成相机相对坐标）。 */
    public WireVisualCanvas canvas() {
        return canvas;
    }

    public ClientLevel level() {
        return level;
    }

    /** 本帧正在渲染的维度；跨维度的线路只有这一端要画。 */
    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public LocalPlayer player() {
        return player;
    }

    /** 相机世界坐标（与 {@code CameraRenderState#pos} 一致，已含部分刻插值）。 */
    public Vec3 camera() {
        return camera;
    }

    /** 相机朝向单位向量。 */
    public Vec3 look() {
        return look;
    }

    /** 动画时间（秒，取自系统时钟），用于呼吸 / 闪烁。 */
    public double timeSeconds() {
        return canvas.timeSeconds();
    }

    /** 眼睛位置，做"准星射线"命中判定时用。 */
    public Vec3 eye() {
        return player.getEyePosition();
    }

    /** 某点到相机的距离（格）。 */
    public double distanceFromCamera(Vec3 point) {
        return camera.distanceTo(point);
    }

    /** 某点是否还在给定距离内（附属模组自己做粗剔除时可用）。 */
    public boolean within(double blocks, Vec3 point) {
        return camera.distanceToSqr(point) <= blocks * blocks;
    }

    /** 呼吸系数（0..1）：默认 2 秒一个周期，与连接装置的待选点提示一致。 */
    public float breathe(float periodSeconds, float phaseOffset) {
        double phase = (timeSeconds() / periodSeconds) + phaseOffset;
        return (float) (0.5 + 0.5 * Math.sin(phase * Math.PI * 2.0));
    }
}
