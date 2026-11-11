package cn.blockforge.generated.origoldsar.api.visual;

/**
 * 无线视觉元素的绘制层次。
 *
 * <p>模块一共只有 4 个几何批次：{@code (核心 | 辉光) × (遮挡 | 穿墙)}，
 * 由 {@link WireVisuals} 在每帧提交给 26.1.2 的渲染特性管线。
 * 附属模组（如《OriGolds: Industrialized Age》的金源能量电线 / 无线物流管道）
 * 只需要在 {@link WireVisualStyle} 里挑一个层次，不用关心渲染细节。
 */
public enum WireVisualLayer {
    /** 与地形做深度测试：被方块挡住的线段不显示，适合贴着实物画的线。 */
    OCCLUDED,
    /** 关掉深度测试：穿墙显示。无线链路本来就是"看不见的连接"，默认走这一层。 */
    OVERLAY
}
