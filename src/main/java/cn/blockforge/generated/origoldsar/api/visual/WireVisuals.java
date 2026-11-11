package cn.blockforge.generated.origoldsar.api.visual;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 无线视觉模块入口（1.21.1 版）：通道注册表与 26.x 版完全一致，几何也由同一份
 * {@link WireVisualCanvas} 生成；区别只在最后一步——1.21.1 还是即时模式渲染，
 * 这里在 {@code AFTER_WEATHER} 阶段用 {@code POSITION_COLOR + QUADS} 直接画四批：
 *
 * <table border="1">
 *   <caption>绘制顺序（均关闭背面剔除、不写深度）</caption>
 *   <tr><th>批次</th><th>混合</th><th>深度</th></tr>
 *   <tr><td>辉光 / 遮挡层</td><td>加法</td><td>LEQUAL</td></tr>
 *   <tr><td>核心 / 遮挡层</td><td>半透明</td><td>LEQUAL</td></tr>
 *   <tr><td>辉光 / 穿墙层</td><td>加法</td><td>关闭深度测试</td></tr>
 *   <tr><td>核心 / 穿墙层</td><td>半透明</td><td>关闭深度测试</td></tr>
 * </table>
 *
 * <p>本模组的无线红石 / 遥控开关通道只在客户端配置 {@code wireStyle = NEO} 时产生几何
 * （见 {@code WirelessConnectorClient}）。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WireVisuals {
    private static final Logger LOGGER = LoggerFactory.getLogger("origolds_ar/WireVisuals");

    /** 超过这个距离（格）的无线视觉几何不再产生顶点。 */
    public static final double CULL_DISTANCE = 192.0;

    private static final WireVisualCanvas CANVAS = new WireVisualCanvas(CULL_DISTANCE);

    private static volatile WireVisualSource[] ordered = new WireVisualSource[0];
    private static final Map<ResourceLocation, WireVisualSource> channels = new LinkedHashMap<>();
    private static final List<ResourceLocation> muted = new ArrayList<>();

    private WireVisuals() {}

    // ---------------------------------------------------------------- 全局外观旋钮

    public static float baseWidth() {
        return WireVisualStyle.baseWidth();
    }

    public static void setBaseWidth(float blocks) {
        WireVisualStyle.setBaseWidth(blocks);
    }

    public static void resetBaseWidth() {
        WireVisualStyle.resetBaseWidth();
    }

    // ---------------------------------------------------------------- 通道注册表

    public static synchronized void register(WireVisualSource source) {
        channels.put(source.id(), source);
        muted.remove(source.id());
        rebuild();
    }

    public static void register(ResourceLocation id, int priority, Predicate<WireVisualContext> active,
                                Consumer<WireVisualContext> collect) {
        register(WireVisualSource.of(id, priority, active, collect));
    }

    public static void register(ResourceLocation id, Consumer<WireVisualContext> collect) {
        register(WireVisualSource.of(id, collect));
    }

    public static synchronized void unregister(ResourceLocation id) {
        channels.remove(id);
        muted.remove(id);
        rebuild();
    }

    public static synchronized boolean isRegistered(ResourceLocation id) {
        return channels.containsKey(id);
    }

    public static List<WireVisualSource> sources() {
        return List.of(ordered);
    }

    private static void rebuild() {
        List<WireVisualSource> list = new ArrayList<>(channels.values());
        list.sort(Comparator.comparingInt(WireVisualSource::priority).thenComparing(s -> s.id().toString()));
        ordered = list.toArray(new WireVisualSource[0]);
    }

    // ---------------------------------------------------------------- 每帧绘制

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        WireVisualSource[] snapshot = ordered;
        if (snapshot.length == 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        Vector3f lookVector = event.getCamera().getLookVector();
        Vec3 look = new Vec3(lookVector.x, lookVector.y, lookVector.z);
        CANVAS.beginFrame(cam, System.currentTimeMillis() / 1000.0);
        WireVisualContext context = new WireVisualContext(level, level.dimension(), minecraft.player,
                cam, look, CANVAS);

        for (WireVisualSource source : snapshot) {
            if (muted.contains(source.id())) {
                continue;
            }
            try {
                if (source.isActive(context)) {
                    source.collect(context);
                }
            } catch (RuntimeException | Error error) {
                if (!muted.contains(source.id())) {
                    muted.add(source.id());
                    LOGGER.warn("无线视觉通道 {} 在收集几何时抛出异常，已停用这一路（其余通道不受影响）",
                            source.id(), error);
                }
            }
        }
        if (CANVAS.isEmpty()) {
            return;
        }

        // 画布里的坐标已经是“世界坐标 - 相机坐标”，事件给的位姿只含相机旋转，直接用即可。
        PoseStack.Pose pose = event.getPoseStack().last();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            draw(pose, CANVAS.batch(1, WireVisualLayer.OCCLUDED), true);
            draw(pose, CANVAS.batch(0, WireVisualLayer.OCCLUDED), false);

            RenderSystem.disableDepthTest();
            draw(pose, CANVAS.batch(1, WireVisualLayer.OVERLAY), true);
            draw(pose, CANVAS.batch(0, WireVisualLayer.OVERLAY), false);
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static void draw(PoseStack.Pose pose, WireVisualCanvas.QuadBatch batch, boolean additive) {
        if (batch.isEmpty()) {
            return;
        }
        if (additive) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        batch.writeTo(pose, buffer);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
