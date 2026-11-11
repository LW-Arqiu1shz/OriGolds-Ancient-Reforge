package cn.blockforge.generated.origoldsar.api.visual;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * "一条线路"的通用形状：两个端点 + 各自所在维度 + 可选的自定义样式。
 *
 * <p>本模组的无线红石线路是它，附属模组的金源能量电线、无线物流管道也可以是它——
 * 直接让自己现有的线路记录实现本接口，或者用 {@link #of} 现造一个轻量视图即可，
 * 之后交给 {@link WireVisualLinks#render} 一次画完整批线路。
 */
public interface WireVisualLink {
    /** 线路 id，用于悬停 / 删除一类的交互（没有 id 的概念时可以固定返回 {@link UUID#randomUUID()} 缓存值）。 */
    UUID id();

    ResourceKey<Level> dimStart();

    BlockPos posStart();

    ResourceKey<Level> dimEnd();

    BlockPos posEnd();

    /** 线体样式；返回 {@code null} 表示用本批线路的默认样式。 */
    default WireVisualStyle lineStyle() {
        return null;
    }

    /** 端点样式；返回 {@code null} 表示按线体两端颜色各画一端。 */
    default WireVisualStyle nodeStyle() {
        return null;
    }

    /** 线路中点（同维度时才有意义）。 */
    default Vec3 centerStart() {
        return Vec3.atCenterOf(posStart());
    }

    default Vec3 centerEnd() {
        return Vec3.atCenterOf(posEnd());
    }

    /** 快速造一个只读视图（附属模组不想为自己的线路类加 implements 时用）。 */
    static WireVisualLink of(UUID id, ResourceKey<Level> dimA, BlockPos a,
                             ResourceKey<Level> dimB, BlockPos b, WireVisualStyle lineStyle) {
        return new WireVisualLink() {
            @Override
            public UUID id() {
                return id;
            }

            @Override
            public ResourceKey<Level> dimStart() {
                return dimA;
            }

            @Override
            public BlockPos posStart() {
                return a;
            }

            @Override
            public ResourceKey<Level> dimEnd() {
                return dimB;
            }

            @Override
            public BlockPos posEnd() {
                return b;
            }

            @Override
            public WireVisualStyle lineStyle() {
                return lineStyle;
            }

            @Override
            public WireVisualStyle nodeStyle() {
                return null;
            }
        };
    }
}
