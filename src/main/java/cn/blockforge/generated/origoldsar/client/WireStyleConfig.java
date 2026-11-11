package cn.blockforge.generated.origoldsar.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置 {@code config/origolds_ar-client.toml}：
 * <ul>
 *   <li>{@code wireStyle}：无线线路样式。
 *     <ul>
 *       <li>{@code CLASSIC}：1.21.1 原来的样式——1 像素渐变线 + 方块描边 / 半透明填充；</li>
 *       <li>{@code NEO}（默认）：与 26.x 版一致的样式——面向相机的渐变线带 + 流光 + 辉光、
 *           四角括号、呼吸提示框。</li>
 *     </ul>
 *   </li>
 * </ul>
 * 也可以在“模组”列表里点本模组的“配置”按钮修改，立即生效，不用重进世界。
 */
public final class WireStyleConfig {
    public enum WireStyle {
        CLASSIC,
        NEO
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<WireStyle> WIRE_STYLE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        WIRE_STYLE = builder
                .comment("Wireless wire style. CLASSIC = original 1.21.1 thin lines; NEO = glowing ribbons (same as the 26.x version).",
                        "无线线路样式：CLASSIC = 1.21.1 原版细线；NEO = 发光线带（与 26.x 版一致）。")
                .translation("origolds_ar.configuration.wireStyle")
                .defineEnum("wireStyle", WireStyle.NEO);
        SPEC = builder.build();
    }

    private WireStyleConfig() {
    }

    /** 当前是否使用 NEO 样式。配置文件尚未加载（极早期）时按默认值 NEO 处理。 */
    public static boolean isNeo() {
        try {
            return WIRE_STYLE.get() == WireStyle.NEO;
        } catch (IllegalStateException notLoadedYet) {
            return true;
        }
    }
}
