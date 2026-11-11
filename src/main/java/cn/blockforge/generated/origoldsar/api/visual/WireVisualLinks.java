package cn.blockforge.generated.origoldsar.api.visual;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * "一批线路"的通用画法：线体 + 两端端点 + 悬停高亮，一次画完。
 *
 * <p>本模组的无线红石线路用它，附属模组（《OriGolds: Industrialized Age》）的
 * 金源能量电线、无线物流管道也直接用它——只要让自己的线路记录实现 {@link WireVisualLink}
 * （或者用 {@link WireVisualLink#of} 现造视图）：
 *
 * <pre>{@code
 * UUID hovered = WireVisualLinks.hover(ctx, myWires, RANGE, DEFAULT_HOVER_THRESHOLD);
 * WireVisualLinks.render(ctx, myWires, MY_LINE_STYLE, null, hovered);
 * }</pre>
 *
 * <p>{@link #hover} 与 {@link #render} 用的是同一个相机（{@link WireVisualContext#camera()} /
 * {@link WireVisualContext#look()}），所以"看着亮了"和"按下去删掉"必然指同一条线。
 */
public final class WireVisualLinks {
    private WireVisualLinks() {}

    /**
     * 准星射线在 {@code range} 格内最贴近哪一条线路（穿墙几何判定，无视中间遮挡）。
     *
     * @param threshold 允许的准星与线段的最短距离（格），一般用
     *                  {@link WireVisualMath#DEFAULT_HOVER_THRESHOLD}
     * @return 命中的线路 id，一条都没有则 {@code null}
     */
    public static UUID hover(WireVisualContext context, Iterable<? extends WireVisualLink> links,
                             double range, double threshold) {
        ResourceKey<Level> dim = context.dimension();
        Vec3 eye = context.eye();
        Vec3 look = context.look();
        double thresholdSq = threshold * threshold;
        UUID best = null;
        double bestDistSq = thresholdSq;
        for (WireVisualLink link : links) {
            // 跨维度的线路画不出线体，自然也不该被"指着"。
            if (!link.dimStart().equals(dim) || !link.dimEnd().equals(dim)) {
                continue;
            }
            double distSq = WireVisualMath.rayToSegmentDistanceSq(
                    eye, look, link.posStart(), link.posEnd(), range);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = link.id();
            }
        }
        return best;
    }

    /**
     * 画一批线路。
     *
     * @param defaultLine 线路没自带样式（{@link WireVisualLink#lineStyle()} 返回 {@code null}）时使用的线体样式
     * @param defaultNode 端点样式；两者都给 {@code null} 时按"线体两端颜色各画一端"处理
     * @param hovered     准星正指着的那条线路（整条描白），没有则 {@code null}
     */
    public static void render(WireVisualContext context, Iterable<? extends WireVisualLink> links,
                              WireVisualStyle defaultLine, WireVisualStyle defaultNode, UUID hovered) {
        WireVisualCanvas canvas = context.canvas();
        ResourceKey<Level> dim = context.dimension();
        for (WireVisualLink link : links) {
            boolean startHere = link.dimStart().equals(dim);
            boolean endHere = link.dimEnd().equals(dim);
            if (!startHere && !endHere) {
                continue;
            }
            WireVisualStyle line = link.lineStyle() != null ? link.lineStyle() : defaultLine;
            if (line == null) {
                continue;
            }
            boolean isHovered = hovered != null && hovered.equals(link.id());
            // 悬停：整条线描白，但两端仍保留各自的颜色倾向，玩家才分得清输入 / 输出。
            // BlockPos 入口会在画布内直接计算中心坐标，避免每帧创建两个 Vec3。
            if (startHere && endHere) {
                canvas.wire(link.posStart(), link.posEnd(), isHovered ? line.whiten(0.9F) : line);
            }
            WireVisualStyle nodeStart = nodeStyle(link, defaultNode, line, true, isHovered);
            WireVisualStyle nodeEnd = nodeStyle(link, defaultNode, line, false, isHovered);
            if (startHere) {
                canvas.endpoint(link.posStart(), nodeStart, 0.0F);
            }
            if (endHere) {
                canvas.endpoint(link.posEnd(), nodeEnd, 0.0F);
            }
        }
    }

    private static WireVisualStyle nodeStyle(WireVisualLink link, WireVisualStyle defaultNode,
                                            WireVisualStyle line, boolean start, boolean hovered) {
        WireVisualStyle node = link.nodeStyle() != null ? link.nodeStyle()
                : defaultNode != null ? defaultNode : line.color(start ? line.colorStart() : line.colorEnd());
        return hovered ? node.whiten(0.6F) : node;
    }
}
