package cn.blockforge.generated.origoldsar.api.visual;

import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 一组无线视觉元素的来源，也就是本模块的"通道"。
 *
 * <p>本模组的无线红石线路、遥控开关绑定是一路通道；附属模组（《OriGolds: Industrialized Age》）
 * 的"金源能量电线"、"无线物流管道"各自再注册一路即可，互不影响、也不用改本模组。
 *
 * <p>注册方式：在自己的客户端初始化里调用
 * {@link WireVisuals#register(WireVisualSource)}（或 {@link WireVisuals#register(Identifier, int, Predicate, Consumer)}
 * 的函数式重载），退出时可用 {@link WireVisuals#unregister(Identifier)} 摘掉。
 *
 * <pre>{@code
 * WireVisuals.register(WireVisualSource.of(
 *         Identifier.fromNamespaceAndPath("origolds_ia", "energy_wire"),
 *         1100,
 *         ctx -> OrigoldsIaClient.energyWireDisplay,
 *         ctx -> {
 *             for (EnergyWire wire : OrigoldsIaClient.wires()) {
 *                 ctx.canvas().wire(wire.from(), wire.to(), STYLE);
 *             }
 *         }));
 * }</pre>
 */
public interface WireVisualSource {
    /** 通道的唯一 id（用你自己的命名空间），同 id 重复注册按"替换"处理。 */
    Identifier id();

    /** 绘制顺序：小的先画（同层里后被画的盖在上面）。默认 1000。 */
    default int priority() {
        return 1000;
    }

    /**
     * 本帧是否要画这一路。返回 {@code false} 时 {@link #collect} 不会被调用，
     * 模块也不会为它产生任何顶点——判断玩家手上的物品、自己的显示模式开关都在这里做。
     */
    default boolean isActive(WireVisualContext context) {
        return true;
    }

    /** 往 {@link WireVisualContext#canvas()} 里塞本帧要画的几何。只读游戏状态，不要改动世界。 */
    void collect(WireVisualContext context);

    /** 函数式创建一路通道（附属模组最省事的用法）。 */
    static WireVisualSource of(Identifier id, Consumer<WireVisualContext> collect) {
        return of(id, 1000, context -> true, collect);
    }

    /** 函数式创建一路带开关判断的通道。 */
    static WireVisualSource of(Identifier id, int priority, Predicate<WireVisualContext> active,
                               Consumer<WireVisualContext> collect) {
        return new WireVisualSource() {
            @Override
            public Identifier id() {
                return id;
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public boolean isActive(WireVisualContext context) {
                return active.test(context);
            }

            @Override
            public void collect(WireVisualContext context) {
                collect.accept(context);
            }

            @Override
            public String toString() {
                return "WireVisualSource[" + id + "]";
            }
        };
    }
}
