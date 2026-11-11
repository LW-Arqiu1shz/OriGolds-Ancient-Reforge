package cn.blockforge.generated.origoldsar.api.visual;

/**
 * 无线视觉模块的颜色小工具：一律使用 {@code 0xAARRGGBB}（与原版 {@code GizmoStyle}、
 * 方块实体染色一致），附属模组可以直接传原版算好的颜色。
 */
public final class WireVisualColor {
    private WireVisualColor() {}

    public static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    public static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    public static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    public static int blue(int argb) {
        return argb & 0xFF;
    }

    public static int of(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /** 替换透明度（0..255），保留 RGB。 */
    public static int withAlpha(int argb, int alpha) {
        return of(red(argb), green(argb), blue(argb), alpha);
    }

    /** 按比例缩放透明度，{@code factor} 大于 1 时按 1 截断。 */
    public static int withAlpha(int argb, float factor) {
        int a = (int) Math.max(0.0F, Math.min(255.0F, alpha(argb) * Math.min(1.0F, factor)));
        return withAlpha(argb, a);
    }

    /** 按比例放大亮度（用于呼吸 / 流光），超过 255 的部分截断，透明度不变。 */
    public static int brighten(int argb, float factor) {
        return of(scale(red(argb), factor), scale(green(argb), factor), scale(blue(argb), factor), alpha(argb));
    }

    /** 线性插值（含透明度），{@code t} 会被夹到 0..1。 */
    public static int mix(int from, int to, float t) {
        float k = Math.max(0.0F, Math.min(1.0F, t));
        return of(lerp(red(from), red(to), k), lerp(green(from), green(to), k),
                lerp(blue(from), blue(to), k), lerp(alpha(from), alpha(to), k));
    }

    private static int lerp(int from, int to, float t) {
        return (int) Math.round(from + (to - from) * t);
    }

    private static int scale(int channel, float factor) {
        return (int) Math.max(0.0F, Math.min(255.0F, channel * factor));
    }
}
