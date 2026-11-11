package cn.blockforge.generated.origoldsar.api.visual;

import net.minecraft.world.phys.Vec3;

/**
 * 一条无线视觉元素的绘制样式（不可变）：渐变两端色、线宽、绘制层次、辉光与流光速度。
 *
 * <p>所有 {@code withXxx} / {@code width} / {@code flow} 方法都返回新实例，附属模组可以安全地
 * 共享一个常量样式：
 *
 * <pre>{@code
 * private static final WireVisualStyle ENERGY =
 *         WireVisualStyle.gradient(0xFF2C7BE5, 0xFFE5A02C).widthScale(3.0F).flow(2.5F);
 * frame.canvas().wire(from, to, ENERGY);
 * }</pre>
 *
 * <h2>两种线宽写法</h2>
 * <table border="1">
 *   <caption>线宽模式一览</caption>
 *   <tr><th>写法</th><th>含义</th><th>受全局基准宽度影响？</th></tr>
 *   <tr><td>不调用任何宽度方法</td><td>正好等于基准宽度（1.0 倍）</td><td>是</td></tr>
 *   <tr><td>{@link #widthScale(float)}</td><td>基准宽度的固定倍数（推荐）</td><td>是</td></tr>
 *   <tr><td>{@link #width(float)}</td><td>写死的绝对线宽（格）</td><td>否——单独配置专用</td></tr>
 * </table>
 *
 * <p>基准宽度是<b>全局唯一的线宽旋钮</b>：{@link #setBaseWidth(float)}（或
 * {@link WireVisuals#setBaseWidth(float)}）改一次，所有"跟随默认"的线宽按倍数一起缩放。
 * 本模组自己的无线连接器全部用 {@code widthScale} 表达线宽，因此加配置选项时只需把值
 * 写进基准宽度；确实需要脱离全局缩放的样式再用 {@code width} 单独钉死。
 */
public final class WireVisualStyle {
    /**
     * 出厂默认线宽（格），也是基准宽度的初值：与原版实体描边同量级，
     * 近距离不刺眼、远距离仍看得见。用户改了配置不等于改这个常量——请看 {@link #baseWidth()}。
     */
    public static final float DEFAULT_WIDTH = 0.025F;
    /** 默认辉光宽度倍数（在核心线宽之外再画一层加法混合的粗光带）。 */
    public static final float DEFAULT_GLOW_SCALE = 2.6F;

    /** 绝对线宽位上的哨兵：表示本样式跟随基准宽度，没有钉死数值。 */
    private static final float FOLLOW_BASE = -1.0F;

    /** 运行时基准宽度（格）：所有跟随默认的样式的乘数基准。配置项写这里。 */
    private static volatile float baseWidth = DEFAULT_WIDTH;

    private final int colorStart;
    private final int colorEnd;
    /** 基准宽度倍数：仅在 {@link #fixedWidth} 为哨兵时生效。 */
    private final float widthScale;
    /** &gt;= 0 时是钉死的绝对线宽（格）；{@link #FOLLOW_BASE} 表示跟随基准宽度。 */
    private final float fixedWidth;
    private final WireVisualLayer layer;
    private final float glowScale;
    private final float flowSpeed;

    private WireVisualStyle(int colorStart, int colorEnd, float widthScale, float fixedWidth,
                            WireVisualLayer layer, float glowScale, float flowSpeed) {
        this.colorStart = colorStart;
        this.colorEnd = colorEnd;
        this.widthScale = widthScale;
        this.fixedWidth = fixedWidth;
        this.layer = layer;
        this.glowScale = glowScale;
        this.flowSpeed = flowSpeed;
    }

    /**
     * 基准宽度：线宽的全局旋钮，初值 {@link #DEFAULT_WIDTH}。
     * 跟随默认的样式实际线宽 = 基准宽度 × 该样式的 {@link #widthScale()}。
     */
    public static float baseWidth() {
        return baseWidth;
    }

    /**
     * 设置基准宽度（格）。所有用 {@code widthScale} / 默认宽度的样式立刻按倍数一起变粗或变细，
     * 用 {@link #width(float)} 钉死绝对线宽的样式不受影响——所以附属模组想让玩家配置"一键调粗细"，
     * 只需要在自己的配置里调用本方法。
     *
     * @param blocks 正数；传入非正值时按 {@link #DEFAULT_WIDTH} 处理（配置读坏了也不会看不见线）
     */
    public static void setBaseWidth(float blocks) {
        baseWidth = blocks > 0.0F && Float.isFinite(blocks) ? blocks : DEFAULT_WIDTH;
    }

    /** 把基准宽度恢复成出厂值 {@link #DEFAULT_WIDTH}。 */
    public static void resetBaseWidth() {
        baseWidth = DEFAULT_WIDTH;
    }

    /** 单色样式（线宽 = 基准宽度 × 1.0，跟随全局配置）。 */
    public static WireVisualStyle of(int argb) {
        return new WireVisualStyle(argb, argb, 1.0F, FOLLOW_BASE,
                WireVisualLayer.OVERLAY, DEFAULT_GLOW_SCALE, 0.0F);
    }

    /** 渐变样式：起点用 {@code from}、终点用 {@code to}，天然表示"输入 → 输出"的流向。 */
    public static WireVisualStyle gradient(int from, int to) {
        return new WireVisualStyle(from, to, 1.0F, FOLLOW_BASE,
                WireVisualLayer.OVERLAY, DEFAULT_GLOW_SCALE, 0.0F);
    }

    /** 完全不发光、也不做加法的细线（给附属模组画辅助框用）。 */
    public static WireVisualStyle hairline(int argb) {
        return new WireVisualStyle(argb, argb, 1.0F, FOLLOW_BASE,
                WireVisualLayer.OVERLAY, 0.0F, 0.0F);
    }

    /**
     * 按基准宽度的倍数设线宽（<b>推荐写法</b>：跟随全局配置，玩家调粗细时一起等比缩放）。
     *
     * @param multiplier 正数倍数；1.0 = 基准宽度，2.0 = 两倍基准宽度
     */
    public WireVisualStyle widthScale(float multiplier) {
        float scale = multiplier > 0.0F && Float.isFinite(multiplier) ? multiplier : 1.0F;
        return new WireVisualStyle(colorStart, colorEnd, scale, FOLLOW_BASE,
                layer, glowScale, flowSpeed);
    }

    /**
     * 钉死绝对线宽（格）：这是<b>单独配置</b>的入口，样式从此脱离全局基准宽度。
     * 只在确实需要"这条线永远这么粗"时使用；要做整体可调的线请用 {@link #widthScale(float)}。
     */
    public WireVisualStyle width(float blocks) {
        float fixed = blocks > 0.0F && Float.isFinite(blocks) ? blocks : DEFAULT_WIDTH;
        return new WireVisualStyle(colorStart, colorEnd, widthScale, fixed,
                layer, glowScale, flowSpeed);
    }

    /** 等比缩放两端颜色的透明度（呼吸、淡出都用这个）。 */
    public WireVisualStyle alpha(float factor) {
        return new WireVisualStyle(WireVisualColor.withAlpha(colorStart, factor),
                WireVisualColor.withAlpha(colorEnd, factor), widthScale, fixedWidth,
                layer, glowScale, flowSpeed);
    }

    /** 把两端颜色一起推向纯白（悬停高亮）。 */
    public WireVisualStyle whiten(float amount) {
        return new WireVisualStyle(WireVisualColor.mix(colorStart, 0xFFFFFFFF, amount),
                WireVisualColor.mix(colorEnd, 0xFFFFFFFF, amount), widthScale, fixedWidth,
                layer, glowScale, flowSpeed);
    }

    public WireVisualStyle layer(WireVisualLayer value) {
        return new WireVisualStyle(colorStart, colorEnd, widthScale, fixedWidth,
                value, glowScale, flowSpeed);
    }

    /** {@code scale <= 0} 关闭辉光；否则在核心线外侧画 {@code scale} 倍宽的加法光带。 */
    public WireVisualStyle glow(float scale) {
        return new WireVisualStyle(colorStart, colorEnd, widthScale, fixedWidth,
                layer, scale, flowSpeed);
    }

    /** 流光速度（格/秒）：沿着"输入 → 输出"方向移动的亮带，0 表示静止。 */
    public WireVisualStyle flow(float blocksPerSecond) {
        return new WireVisualStyle(colorStart, colorEnd, widthScale, fixedWidth,
                layer, glowScale, blocksPerSecond);
    }

    /** 换成单色（端点方块盒、外框常常需要与线体不同的颜色）。 */
    public WireVisualStyle color(int argb) {
        return new WireVisualStyle(argb, argb, widthScale, fixedWidth,
                layer, glowScale, flowSpeed);
    }

    public int colorStart() {
        return colorStart;
    }

    public int colorEnd() {
        return colorEnd;
    }

    /**
     * 当前实际生效的线宽（格）：绝对线宽原样返回，跟随默认的样式按基准宽度换算。
     * 渲染层只读这个值，因此全局配置一改，下一帧就生效。
     */
    public float width() {
        return fixedWidth >= 0.0F ? fixedWidth : baseWidth * widthScale;
    }

    /** 该样式的线宽倍数：跟随默认时是声明的倍数；钉死绝对宽度时按当前基准宽度折算。 */
    public float widthScale() {
        float w = width();
        return fixedWidth >= 0.0F ? w / baseWidth : widthScale;
    }

    /** 该样式是否跟随全局基准宽度（{@code false} = 已用 {@link #width(float)} 单独钉死）。 */
    public boolean followsBaseWidth() {
        return fixedWidth < 0.0F;
    }

    public WireVisualLayer layer() {
        return layer;
    }

    public float glowScale() {
        return glowScale;
    }

    public float flowSpeed() {
        return flowSpeed;
    }

    /** 该样式在某个进度位置（0=起点、1=终点）上的实际颜色，已叠加流光。 */
    public int colorAt(float t, double lengthAlong, double timeSeconds) {
        int base = WireVisualColor.mix(colorStart, colorEnd, t);
        if (flowSpeed <= 0.0F) {
            return base;
        }
        // 亮带周期 3 格，沿输入→输出方向移动；用平滑的余弦脉冲，避免断续闪烁。
        double phase = (lengthAlong - timeSeconds * flowSpeed) / 3.0;
        double pulse = 0.55 + 0.45 * Math.cos(phase * Math.PI * 2.0);
        return WireVisualColor.brighten(base, (float) (0.72 + 0.5 * pulse));
    }

    /** 该样式在某个点上的颜色（默认按该点到起点的距离计算流光相位）。 */
    public int colorAt(float t, Vec3 from, Vec3 point, double timeSeconds) {
        return colorAt(t, from.distanceTo(point), timeSeconds);
    }

    @Override
    public String toString() {
        return "WireVisualStyle[" + Integer.toHexString(colorStart) + "->" + Integer.toHexString(colorEnd)
                + ", w=" + width() + (followsBaseWidth() ? "(" + widthScale + "x base)" : "(fixed)")
                + ", " + layer + ", glow=" + glowScale + ", flow=" + flowSpeed + "]";
    }
}
