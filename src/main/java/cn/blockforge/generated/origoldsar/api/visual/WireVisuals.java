package cn.blockforge.generated.origoldsar.api.visual;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.api.distmarker.Dist;
import org.joml.Quaternionf;
import org.joml.Vector3f;
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
 * 无线视觉模块的入口：一路"通道注册表" + 一套自建的四边形渲染管线。
 *
 * <h2>为什么要自建</h2>
 * <p>26.1.2 把渲染改成了"渲染状态 + 提交节点"模型：即时模式绘制（{@code RenderSystem} +
 * {@code Tesselator}）没了，能画线的 {@code RenderTypes.lines()} 又被官方指向了
 * {@link OutputTarget#ITEM_ENTITY_TARGET}（实体/物品离屏缓冲，只在实体阶段被合成一次），
 * 所以在 {@link SubmitCustomGeometryEvent} 里用 {@code lines()} 提交的线路根本进不到主画面——
 * 这就是移植后"无线线路与端点外框不再显示"的直接原因。而 GL 线段本身在很多驱动上被固定为
 * 1 像素宽，远看几乎不可见。
 *
 * <p>本模块因此完全不走 GL 线段：{@link WireVisualCanvas} 把每条线展成<b>面向相机、
 * 宽度随距离补偿的四边形带</b>，再由这里自建的 4 条管线提交到
 * {@link OutputTarget#MAIN_TARGET}：
 *
 * <table border="1">
 *   <caption>管线一览（均为 POSITION_COLOR + QUADS、关闭背面剔除、不写深度）</caption>
 *   <tr><th>批次</th><th>混合</th><th>深度</th></tr>
 *   <tr><td>辉光 / 遮挡层</td><td>加法（发光）</td><td>LESS_THAN_OR_EQUAL</td></tr>
 *   <tr><td>核心 / 遮挡层</td><td>半透明</td><td>LESS_THAN_OR_EQUAL</td></tr>
 *   <tr><td>辉光 / 穿墙层</td><td>加法（发光）</td><td>ALWAYS_PASS（无视遮挡）</td></tr>
 *   <tr><td>核心 / 穿墙层</td><td>半透明</td><td>ALWAYS_PASS（无视遮挡）</td></tr>
 * </table>
 *
 * <h2>附属模组怎么用</h2>
 * <p>只需要注册一路 {@link WireVisualSource}，在 {@code collect} 里往画布塞几何：
 *
 * <pre>{@code
 * // 客户端初始化（例如 modBus.addListener 里）
 * WireVisuals.register(Identifier.fromNamespaceAndPath("origolds_ia", "energy_wire"), 1100,
 *         ctx -> IAClient.energyWireDisplay,
 *         ctx -> WireVisualLinks.render(ctx, IAClient.wires(), IAClient.STYLE, null, IAClient.hovered));
 * }</pre>
 *
 * <p>本模组自己的两路通道（无线红石线路、遥控开关）见
 * {@code WirelessConnectorClient}，与附属用的是同一套公开 API。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class WireVisuals {
    private static final Logger LOGGER = LoggerFactory.getLogger("origolds_ar/WireVisuals");

    /** 超过这个距离（格）的无线视觉几何不再产生顶点。 */
    public static final double CULL_DISTANCE = 192.0;

    /** 本帧共享的画布（模块内部复用，避免每帧分配）。 */
    private static final WireVisualCanvas CANVAS = new WireVisualCanvas(CULL_DISTANCE);

    /** 已注册的通道，按 {@link WireVisualSource#priority()} 排好序的快照（渲染线程只读它）。 */
    private static volatile WireVisualSource[] ordered = new WireVisualSource[0];
    /** 通道 id -> 通道；注册与摘除都在客户端主线程，仍加锁以防附属在别的线程调用。 */
    private static final Map<Identifier, WireVisualSource> channels = new LinkedHashMap<>();
    /** 已经因抛异常而被静音的通道，避免每帧刷屏。 */
    private static final List<Identifier> muted = new ArrayList<>();

    // ---------------------------------------------------------------- 全局外观旋钮（配置项接这里）

    /** 线宽的全局基准（格），初值 {@link WireVisualStyle#DEFAULT_WIDTH}。等价于 {@code WireVisualStyle.baseWidth()}。 */
    public static float baseWidth() {
        return WireVisualStyle.baseWidth();
    }

    /**
     * 设置全局基准线宽：本模组与所有附属模组里用 {@link WireVisualStyle#widthScale(float)}
     * （或压根没调宽度方法）表达的样式，下一帧起就按这个值等比缩放。
     *
     * <p>用 {@link WireVisualStyle#width(float)} 钉死绝对线宽的样式不受影响，那是给
     * "必须永远这么粗"的元素留的单独配置口。
     *
     * @param blocks 正数；非正值按 {@link WireVisualStyle#DEFAULT_WIDTH} 兜底
     */
    public static void setBaseWidth(float blocks) {
        WireVisualStyle.setBaseWidth(blocks);
    }

    /** 恢复出厂基准线宽。 */
    public static void resetBaseWidth() {
        WireVisualStyle.resetBaseWidth();
    }

    // ---------------------------------------------------------------- 自建管线

    private static final RenderPipeline PIPE_GLOW_OCCLUDED =
            pipeline("wire_visual_glow_occluded", BlendFunction.ADDITIVE, CompareOp.LESS_THAN_OR_EQUAL);
    private static final RenderPipeline PIPE_CORE_OCCLUDED =
            pipeline("wire_visual_core_occluded", BlendFunction.TRANSLUCENT, CompareOp.LESS_THAN_OR_EQUAL);
    private static final RenderPipeline PIPE_GLOW_OVERLAY =
            pipeline("wire_visual_glow_overlay", BlendFunction.ADDITIVE, CompareOp.ALWAYS_PASS);
    private static final RenderPipeline PIPE_CORE_OVERLAY =
            pipeline("wire_visual_core_overlay", BlendFunction.TRANSLUCENT, CompareOp.ALWAYS_PASS);

    /** 提交顺序即叠放顺序：先辉光后核心，核心才能干净地压在自家光晕上。 */
    private static final RenderType TYPE_GLOW_OCCLUDED = type("wire_visual_glow_occluded", PIPE_GLOW_OCCLUDED);
    private static final RenderType TYPE_CORE_OCCLUDED = type("wire_visual_core_occluded", PIPE_CORE_OCCLUDED);
    private static final RenderType TYPE_GLOW_OVERLAY = type("wire_visual_glow_overlay", PIPE_GLOW_OVERLAY);
    private static final RenderType TYPE_CORE_OVERLAY = type("wire_visual_core_overlay", PIPE_CORE_OVERLAY);

    private WireVisuals() {}

    /**
     * 由模组构造器挂到 MOD 总线上（{@code modBus.addListener(WireVisuals::registerPipelines)}）：
     * 26.1.2 的自定义管线必须先注册，首次使用时才拿得到编译好的着色器程序。
     */
    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPE_GLOW_OCCLUDED);
        event.registerPipeline(PIPE_CORE_OCCLUDED);
        event.registerPipeline(PIPE_GLOW_OVERLAY);
        event.registerPipeline(PIPE_CORE_OVERLAY);

        // Iris 26.1.x does not currently provide shader programs for vanilla DEBUG_* pipelines.
        // These are our own pipeline IDs, so register them through Iris' public API without
        // introducing a hard dependency. BASIC supports the POSITION_COLOR geometry used here.
        registerWithIris();
    }

    /**
     * Optional Iris shader-pack integration. A reflection-only bridge keeps Iris optional and
     * lets the exact same geometry render with or without a shader pack.
     */
    private static void registerWithIris() {
        final Class<?> apiClass;
        final Class<?> programClass;
        try {
            apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi", false,
                    WireVisuals.class.getClassLoader());
            programClass = Class.forName("net.irisshaders.iris.api.v0.IrisProgram", false,
                    WireVisuals.class.getClassLoader());
        } catch (ClassNotFoundException absent) {
            return; // Iris is not installed; normal Minecraft pipelines remain in use.
        } catch (LinkageError incompatible) {
            LOGGER.warn("检测到 Iris，但无法加载其兼容 API；无线视觉仍按原版渲染", incompatible);
            return;
        }

        try {
            Object iris = apiClass.getMethod("getInstance").invoke(null);
            Object basicProgram = programClass.getField("BASIC").get(null);
            java.lang.reflect.Method assignPipeline = apiClass.getMethod(
                    "assignPipeline", RenderPipeline.class, programClass);

            assignPipeline.invoke(iris, PIPE_GLOW_OCCLUDED, basicProgram);
            assignPipeline.invoke(iris, PIPE_CORE_OCCLUDED, basicProgram);
            assignPipeline.invoke(iris, PIPE_GLOW_OVERLAY, basicProgram);
            assignPipeline.invoke(iris, PIPE_CORE_OVERLAY, basicProgram);
            LOGGER.info("Iris 已接管 OGAR 无线视觉管线（BASIC 程序）");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            LOGGER.warn("无法向 Iris 注册 OGAR 无线视觉管线；关闭光影时仍可正常渲染", error);
        }
    }

    private static RenderPipeline pipeline(String name, BlendFunction blend, CompareOp depthTest) {
        // 以原版 debug 四边形管线为蓝本：复用它的 POSITION_COLOR 着色器，只改混合与深度。
        return RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "pipeline/" + name))
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                .withColorTargetState(new ColorTargetState(blend))
                .withDepthStencilState(new DepthStencilState(depthTest, false, 0.0F, 0.0F))
                .withCull(false)
                .build();
    }

    private static RenderType type(String name, RenderPipeline pipeline) {
        return RenderType.create(GeneratedMod.MOD_ID + ":" + name,
                RenderSetup.builder(pipeline)
                        // 关键：直接画到主画面，不进实体描边那张离屏缓冲。
                        .setOutputTarget(OutputTarget.MAIN_TARGET)
                        .setLayeringTransform(LayeringTransform.NO_LAYERING)
                        .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                        .createRenderSetup());
    }

    // ---------------------------------------------------------------- 通道注册表

    /** 注册一路通道（同 id 重复注册按替换处理，附属重载模组时可直接重进游戏）。 */
    public static synchronized void register(WireVisualSource source) {
        channels.put(source.id(), source);
        muted.remove(source.id());
        rebuild();
    }

    /** 函数式注册（附属模组最省事的写法）：id + 绘制顺序 + 开关判断 + 绘制回调。 */
    public static void register(Identifier id, int priority, Predicate<WireVisualContext> active,
                                Consumer<WireVisualContext> collect) {
        register(WireVisualSource.of(id, priority, active, collect));
    }

    /** 函数式注册，绘制顺序用默认值 1000、且始终启用。 */
    public static void register(Identifier id, Consumer<WireVisualContext> collect) {
        register(WireVisualSource.of(id, collect));
    }

    /** 摘掉一路通道（附属自己的显示模式全关时可以不摘——{@code isActive} 返回 false 就不产生顶点）。 */
    public static synchronized void unregister(Identifier id) {
        channels.remove(id);
        muted.remove(id);
        rebuild();
    }

    /** 某路通道是否已注册（附属可用来判断桥接是否已经接上）。 */
    public static synchronized boolean isRegistered(Identifier id) {
        return channels.containsKey(id);
    }

    /** 当前注册的通道（按绘制顺序），只用于调试。 */
    public static List<WireVisualSource> sources() {
        return List.of(ordered);
    }

    private static void rebuild() {
        List<WireVisualSource> list = new ArrayList<>(channels.values());
        list.sort(Comparator.comparingInt(WireVisualSource::priority).thenComparing(s -> s.id().toString()));
        ordered = list.toArray(new WireVisualSource[0]);
    }

    // ---------------------------------------------------------------- 每帧提交

    /**
     * 性能二分开关（JVM 参数，默认全部关闭＝正常渲染，不影响任何玩家）：
     *
     * <ul>
     *   <li>{@code -Dorigolds_ar.wire_visual=off}：整套无线视觉（本模组与附属共用的通道系统）
     *       一帧不提交——光影下帧率若回到 120，问题就在这条提交链上；</li>
     *   <li>{@code -Dorigolds_ar.wire_visual=glow_off}：只画核心、不画辉光带（辉光是半透明
     *       加法混合的宽条带，光影下按像素计费，是 GPU 侧的头号嫌疑）；</li>
     *   <li>{@code -Dorigolds_ar.wire_visual=occluded_only}：穿墙层强制退回带深度测试的
     *       遮挡层（不再产生 order(1) 的晚提交桶，验证 ALWAYS_PASS 晚提交的成本）；</li>
     *   <li>{@code -Dorigolds_ar.wire_visual_stats=true}：每 120 帧往日志打一行
     *       通道数 / 顶点数 / 几何收集耗时，用来分离 CPU 与 GPU。</li>
     * </ul>
     */
    private static final String DEBUG_MODE = System.getProperty("origolds_ar.wire_visual", "on");
    private static final boolean STATS = "true".equalsIgnoreCase(System.getProperty("origolds_ar.wire_visual_stats", ""));
    private static long statsFrames;
    private static long statsCollectNanos;
    private static int statsVertices;

    /**
     * 每帧收拢各路通道的几何并提交。相机的位置与朝向取自
     * {@code LevelRenderState#cameraRenderState}，因此画布里的坐标一律是"世界坐标 - 相机坐标"，
     * 提交时给一个不带平移的位姿即可。
     */
    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        if (DEBUG_MODE.equalsIgnoreCase("off")) {
            return;
        }
        WireVisualCanvas.suppressGlow = DEBUG_MODE.equalsIgnoreCase("glow_off");
        WireVisualCanvas.forceOccluded = DEBUG_MODE.equalsIgnoreCase("occluded_only");
        WireVisualSource[] snapshot = ordered;
        if (snapshot.length == 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        Vec3 cam = camera.pos;
        long t0 = STATS ? System.nanoTime() : 0L;
        CANVAS.beginFrame(cam, System.currentTimeMillis() / 1000.0);
        WireVisualContext context = new WireVisualContext(level, level.dimension(), minecraft.player,
                cam, lookOf(camera), CANVAS);

        for (WireVisualSource source : snapshot) {
            if (muted.contains(source.id())) {
                continue; // 之前抛过异常的通道不再调用（注册同 id 的新通道会重新启用）
            }
            try {
                if (source.isActive(context)) {
                    source.collect(context);
                }
            } catch (RuntimeException | Error error) {
                // 附属通道写崩了不能连累本模组渲染，更不能刷屏：每路只报一次。
                if (muted.add(source.id())) {
                    LOGGER.warn("无线视觉通道 {} 在收集几何抛出异常，已停用这一路（其余通道不受影响）",
                            source.id(), error);
                }
            }
        }
        if (STATS) {
            statsFrames++;
            statsCollectNanos += System.nanoTime() - t0;
            statsVertices += CANVAS.totalVertices();
            if (statsFrames >= 120) {
                LOGGER.info("[WireVisuals] {} 帧均值：几何收集 {} µs/帧，顶点 {} 个/帧，通道 {} 路{}",
                        statsFrames, statsCollectNanos / statsFrames / 1000,
                        statsVertices / statsFrames, snapshot.length,
                        CANVAS.isEmpty() ? "（本段无提交）" : "");
                statsFrames = 0;
                statsCollectNanos = 0;
                statsVertices = 0;
            }
        }

        if (CANVAS.isEmpty()) {
            return;
        }
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        PoseStack poseStack = event.getPoseStack();

        // order 桶惰性创建：只有该层真的有几何才建桶，避免每帧多出一个空提交桶 / 空渲染段。
        WireVisualCanvas.QuadBatch glowOccluded = CANVAS.batch(1, WireVisualLayer.OCCLUDED);
        WireVisualCanvas.QuadBatch coreOccluded = CANVAS.batch(0, WireVisualLayer.OCCLUDED);
        if (!glowOccluded.isEmpty() || !coreOccluded.isEmpty()) {
            // Keep ordinary geometry in the normal feature order so it respects world occlusion.
            OrderedSubmitNodeCollector normalOrder = collector.order(0);
            submit(normalOrder, poseStack, glowOccluded, TYPE_GLOW_OCCLUDED);
            submit(normalOrder, poseStack, coreOccluded, TYPE_CORE_OCCLUDED);
        }

        // Submit see-through geometry after the regular world features. ALWAYS_PASS disables
        // depth rejection; the later order also prevents subsequently rendered world geometry
        // from painting over the wireless line in the same frame.
        WireVisualCanvas.QuadBatch glowOverlay = CANVAS.batch(1, WireVisualLayer.OVERLAY);
        WireVisualCanvas.QuadBatch coreOverlay = CANVAS.batch(0, WireVisualLayer.OVERLAY);
        if (!glowOverlay.isEmpty() || !coreOverlay.isEmpty()) {
            OrderedSubmitNodeCollector overlayOrder = collector.order(1);
            submit(overlayOrder, poseStack, glowOverlay, TYPE_GLOW_OVERLAY);
            submit(overlayOrder, poseStack, coreOverlay, TYPE_CORE_OVERLAY);
        }
    }

    private static void submit(OrderedSubmitNodeCollector collector, PoseStack poseStack,
                               WireVisualCanvas.QuadBatch batch, RenderType renderType) {
        if (batch.isEmpty()) {
            return;
        }
        collector.submitCustomGeometry(poseStack, renderType, batch::writeTo);
    }

    /** 相机朝向：复刻 {@code Camera.setRotation} 里 {@code rotationYXZ(π - yRot, -xRot, -roll)} 作用在 (0,0,-1) 上。 */
    static Vec3 lookOf(CameraRenderState camera) {
        Quaternionf rotation = new Quaternionf().rotationYXZ(
                (float) Math.PI - camera.yRot * (float) (Math.PI / 180.0),
                -camera.xRot * (float) (Math.PI / 180.0),
                0.0F);
        Vector3f forwards = new Vector3f(0.0F, 0.0F, -1.0F).rotate(rotation);
        return new Vec3(forwards.x, forwards.y, forwards.z);
    }
}
