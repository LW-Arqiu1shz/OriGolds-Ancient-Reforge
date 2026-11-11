package cn.blockforge.generated.origoldsar.api.visual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * 无线视觉模块用到的纯几何工具。
 *
 * <p>这些方法之所以放在模块里，是因为"悬停哪条线路 / 准星穿墙选中哪个方块"这类交互
 * 必须和渲染用同一套相机数据（{@link WireVisualContext#camera()} 与
 * {@link WireVisualContext#look()}），否则会出现"看着亮了却删不掉"的错位。
 * 附属模组直接调用同样的方法，就能得到与本模组一致的手感。
 */
public final class WireVisualMath {
    /** 射线与线段的最小可接受距离（格）：在这个范围内算悬停。 */
    public static final double DEFAULT_HOVER_THRESHOLD = 0.6;

    private WireVisualMath() {}

    /** 射线（origin + t*dir，t>0）到线段 ab 的最短距离的平方（不限长度）。 */
    public static double rayToSegmentDistanceSq(Vec3 origin, Vec3 dir, Vec3 a, Vec3 b) {
        return rayToSegmentDistanceSq(origin, dir, a, b, Double.MAX_VALUE);
    }

    /**
     * 射线到线段的最短距离平方，只考虑射线上 {@code maxRange} 格以内、且线段前方（along>0）的部分。
     * 与准星交互配套：线段远端超出射程时不算悬停。
     */
    public static double rayToSegmentDistanceSq(Vec3 origin, Vec3 dir, Vec3 a, Vec3 b, double maxRange) {
        Vec3 ab = b.subtract(a);
        double lenSq = ab.lengthSqr();
        if (lenSq < 1.0E-8) {
            return distancePointToRaySq(origin, dir, a, maxRange);
        }
        // 在线段上采样：比解析解更省代码，且天然只算射程以内的部分。
        int steps = (int) Math.max(8, Math.min(160, Math.ceil(ab.length() * 2.0)));
        double best = Double.MAX_VALUE;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 p = a.add(ab.x * t, ab.y * t, ab.z * t);
            double d = distancePointToRaySq(origin, dir, p, maxRange);
            if (d < best) {
                best = d;
            }
        }
        return best;
    }

    /**
     * 方块端点版射线 / 线段距离：保持原来的采样判定，但在标量上完成整个循环。
     * 无线网络每帧会对每条线路做一次悬停测试；原版 Vec3 写法会在每个采样点分配
     * 多个对象，这个重载避免把 GC 压力随线路长度和数量线性放大。
     */
    public static double rayToSegmentDistanceSq(Vec3 origin, Vec3 dir,
                                                BlockPos start, BlockPos end, double maxRange) {
        double ax = start.getX() + 0.5, ay = start.getY() + 0.5, az = start.getZ() + 0.5;
        double bx = end.getX() + 0.5, by = end.getY() + 0.5, bz = end.getZ() + 0.5;
        double abx = bx - ax, aby = by - ay, abz = bz - az;
        double lenSq = abx * abx + aby * aby + abz * abz;
        double rangeSq = maxRange * maxRange;
        if (lenSq < 1.0E-8) {
            return distancePointToRaySq(origin, dir, ax, ay, az, rangeSq);
        }

        int steps = (int) Math.max(8, Math.min(160, Math.ceil(Math.sqrt(lenSq) * 2.0)));
        double best = Double.MAX_VALUE;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double px = ax + abx * t, py = ay + aby * t, pz = az + abz * t;
            double vx = px - origin.x, vy = py - origin.y, vz = pz - origin.z;
            double along = vx * dir.x + vy * dir.y + vz * dir.z;
            double pointDistSq = vx * vx + vy * vy + vz * vz;
            if (along <= 0.05 || pointDistSq > rangeSq) {
                continue;
            }
            double distanceSq = Math.max(0.0, pointDistSq - along * along);
            if (distanceSq < best) {
                best = distanceSq;
            }
        }
        return best;
    }

    private static double distancePointToRaySq(Vec3 origin, Vec3 dir,
                                               double pointX, double pointY, double pointZ,
                                               double rangeSq) {
        double vx = pointX - origin.x, vy = pointY - origin.y, vz = pointZ - origin.z;
        double along = vx * dir.x + vy * dir.y + vz * dir.z;
        double distanceSq = vx * vx + vy * vy + vz * vz;
        if (along <= 0.05 || distanceSq > rangeSq) {
            return Double.MAX_VALUE;
        }
        return Math.max(0.0, distanceSq - along * along);
    }

    /** 点到射线的距离平方；射线后方的点、以及超出 {@code maxRange} 的点都返回 {@link Double#MAX_VALUE}。 */
    public static double distancePointToRaySq(Vec3 origin, Vec3 dir, Vec3 point, double maxRange) {
        Vec3 v = point.subtract(origin);
        double along = v.dot(dir);
        if (along <= 0.05 || v.lengthSqr() > maxRange * maxRange) {
            return Double.MAX_VALUE;
        }
        return Math.max(0.0, v.lengthSqr() - along * along);
    }

    /** 点到射线的距离平方（不限射程）。 */
    public static double distancePointToRaySq(Vec3 origin, Vec3 dir, Vec3 point) {
        return distancePointToRaySq(origin, dir, point, Double.MAX_VALUE);
    }

    /**
     *  slab 法射线 / 方块求交：无视中间遮挡（"穿墙选中"），只要射线在
     * {@code range} 格内掠过该方块就算命中。
     */
    public static boolean rayHitsBlock(Vec3 origin, Vec3 dir, BlockPos pos, double range) {
        return rayHitsAabb(origin, dir, WireVisualCanvas.blockBox(pos, 0.05), range);
    }

    /** 射线 / 轴对齐盒求交（slab 法）。 */
    public static boolean rayHitsAabb(Vec3 origin, Vec3 dir, net.minecraft.world.phys.AABB box, double range) {
        double[] o = {origin.x, origin.y, origin.z};
        double[] d = {dir.x, dir.y, dir.z};
        double[] loB = {box.minX, box.minY, box.minZ};
        double[] hiB = {box.maxX, box.maxY, box.maxZ};
        double lo = 0.0;
        double hi = range;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1.0E-8) {
                if (o[i] < loB[i] || o[i] > hiB[i]) {
                    return false;
                }
            } else {
                double t1 = (loB[i] - o[i]) / d[i];
                double t2 = (hiB[i] - o[i]) / d[i];
                if (t1 > t2) {
                    double t = t1;
                    t1 = t2;
                    t2 = t;
                }
                lo = Math.max(lo, t1);
                hi = Math.min(hi, t2);
                if (lo > hi) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 把 packed 方块坐标（{@code BlockPos#asLong} 的结果）还原成方块中心点。 */
    public static Vec3 centerOf(long packedPos) {
        return Vec3.atCenterOf(BlockPos.of(packedPos));
    }
}
