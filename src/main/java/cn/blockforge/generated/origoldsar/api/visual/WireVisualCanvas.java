package cn.blockforge.generated.origoldsar.api.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 无线视觉模块的绘制面（画布）：把"线段 / 端点 / 外框 / 填充"这些高层次意图
 * 展开成 26.1.2 渲染管线需要的四边形顶点。
 *
 * <p>模块自己完成了原本要靠 GL 线段（{@code RenderTypes.lines()}）做的所有事情：
 * 线段被展成<b>面向相机的四边形带</b>，宽度随距离补偿，所以它在任何分辨率、任何距离上
 * 都保持稳定的屏幕粗细，也不会再依赖"实体描边"那张离屏缓冲（那正是线路渲染失效的根因）。
 *
 * <p>附属模组不需要自己创建画布：在 {@link WireVisualSource#collect} 里通过
 * {@link WireVisualContext#canvas()} 取本帧的画布即可。
 *
 * <p><b>性能约定</b>（26.1.2 移植后新增）：
 * <ul>
 *   <li>几何热路径（{@link #wire} / {@link #boxOutline} / {@link #cornerBrackets}）全部用
 *       double 标量计算，逐段循环<b>零对象分配</b>；方块端点也直接传 {@link BlockPos}，避免
 *       每帧给每条线创建临时 {@link Vec3}；</li>
 *   <li>剔除<b>只按相机距离</b>（{@link #cullDistanceSq}），<b>不做视锥剔除</b>。曾经加过的视锥剔除
 *       会让视野里的线路整条消失——本模块的几何取自 {@code cameraRenderState#pos}，而
 *       {@code cullFrustum} 是方块可见性流程为另一个相机原点/另一个时刻准备的，两者对不上时
 *       {@code isVisible} 判定就是错的。宁可让 GPU 去裁屏幕外的三角形，也不能漏画。</li>
 *   <li>线路细分步长放宽到 4 格，减半长线的四边形数量；线体颜色、宽度补偿、面朝向与流光采样方式不变。</li>
 * </ul>
 */
public final class WireVisualCanvas {
    /** 屏幕粗细补偿的参考距离：比这更近的几何按真实世界宽度画，更远的按屏幕等宽放大。 */
    private static final double REFERENCE_DISTANCE = 10.0;
    /** 屏幕粗细补偿的最大倍数，避免极远处的线糊成一片。 */
    private static final double MAX_WIDTH_SCALE = 14.0;
    /** 单条线段最多细分成多少个小四边形（细分是为了让"面向相机"在长线上也贴合）。 */
    private static final int MAX_SUBDIVISIONS = 160;
    /** 每个小段的目标长度（格）；从 2 格调到 4 格，降低每条线的顶点生成量。 */
    private static final double SUBDIVISION_LENGTH = 4.0;
    /** 与 {@code Vec3.normalize()} 一致的退化阈值。 */
    private static final double NORMALIZE_EPS = 9.999999747378752E-6;

    /** 4 个几何批：0/1 = 核心的遮挡 / 穿墙层，2/3 = 辉光的遮挡 / 穿墙层。 */
    private final QuadBatch[] batches = new QuadBatch[4];
    private final double cullDistanceSq;

    /** 本帧相机（世界坐标），拆成标量供热路径零分配使用。 */
    private double camX, camY, camZ;
    private Vec3 camera = Vec3.ZERO;
    private double timeSeconds;
    /** 8 个盒角的暂存坐标（每角 3 个 double）。 */
    private final double[] cornerScratch = new double[24];
    /** sideAt 的暂存输出（两条边的横向向量）。 */
    private final double[] sideScratch0 = new double[3];
    private final double[] sideScratch1 = new double[3];

    /** {@code -Dorigolds_ar.wire_visual=glow_off} 时本帧不生成辉光带（只影响显式调试场景）。 */
    static boolean suppressGlow = false;
    /** {@code -Dorigolds_ar.wire_visual=occluded_only}：穿墙层几何并入遮挡层（不再产生晚提交桶）。 */
    static boolean forceOccluded = false;

    /** 调试模式下把穿墙层强制映射为遮挡层；正常情况原样返回。 */
    private static WireVisualLayer effective(WireVisualLayer layer) {
        return forceOccluded ? WireVisualLayer.OCCLUDED : layer;
    }

    /** @param cullDistance 超过这个距离（格）的几何直接丢弃 */
    public WireVisualCanvas(double cullDistance) {
        this.cullDistanceSq = cullDistance * cullDistance;
        for (int i = 0; i < batches.length; i++) {
            batches[i] = new QuadBatch();
        }
    }

    /** 帧首由 {@link WireVisuals} 调用：换相机、换时间、清空上一帧顶点（数组原地复用）。 */
    void beginFrame(Vec3 camera, double timeSeconds) {
        this.camera = camera;
        this.camX = camera.x;
        this.camY = camera.y;
        this.camZ = camera.z;
        this.timeSeconds = timeSeconds;
        for (QuadBatch batch : batches) {
            batch.clear();
        }
    }

    public Vec3 camera() {
        return camera;
    }

    public double timeSeconds() {
        return timeSeconds;
    }

    /** 本帧是否一个顶点都没有（决定要不要提交自定义几何）。 */
    public boolean isEmpty() {
        for (QuadBatch batch : batches) {
            if (!batch.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 本帧顶点总数（调试 / 性能统计用）。 */
    public int totalVertices() {
        int n = 0;
        for (QuadBatch batch : batches) {
            n += batch.vertexCount();
        }
        return n;
    }

    /** 取某个几何批；{@code group}：0 = 核心、1 = 辉光。附属模组一般用不到。 */
    public QuadBatch batch(int group, WireVisualLayer layer) {
        return batches[group * 2 + layer.ordinal()];
    }

    /** 核心批；{@code occluded_only} 调试模式会把穿墙层的写入并进遮挡批。 */
    private QuadBatch core(WireVisualLayer layer) {
        return batches[effective(layer).ordinal()];
    }

    /** 辉光批；调试关闭辉光时返回 null，调用方据此跳过（核心几何不受影响）。 */
    private QuadBatch glow(WireVisualLayer layer) {
        if (suppressGlow) {
            return null;
        }
        return batches[2 + effective(layer).ordinal()];
    }

    // ---------------------------------------------------------------- 高层次绘制入口

    /**
     * 画一条无线线路：渐变核心 + 加法辉光 + 流光。
     *
     * @param from  起点（输入端，世界坐标）
     * @param to    终点（输出端，世界坐标）
     * @param style 线宽、两端颜色、绘制层次、辉光与流光速度
     */
    public void wire(Vec3 from, Vec3 to, WireVisualStyle style) {
        wire(from.x, from.y, from.z, to.x, to.y, to.z, style);
    }

    /** 方块端点版线路：避免无线网络每帧为每条线构造两个临时 Vec3。 */
    public void wire(BlockPos from, BlockPos to, WireVisualStyle style) {
        wire(from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5,
                to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5, style);
    }

    private void wire(double fromX, double fromY, double fromZ,
                      double toX, double toY, double toZ, WireVisualStyle style) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-5 || segmentCulled(fromX, fromY, fromZ, toX, toY, toZ, dx, dy, dz)) {
            return;
        }
        double dirX = dx / length;
        double dirY = dy / length;
        double dirZ = dz / length;
        int steps = (int) Math.max(1, Math.min(MAX_SUBDIVISIONS, Math.ceil(length / SUBDIVISION_LENGTH)));

        QuadBatch core = core(style.layer());
        float gw = style.glowScale();
        QuadBatch glow = (suppressGlow || gw <= 0.0F) ? null : glow(style.layer());

        double width = style.width();
        double glowWidth = width * gw;

        for (int i = 0; i < steps; i++) {
            double t0 = (double) i / steps;
            double t1 = (double) (i + 1) / steps;
            // 与旧实现逐位一致的绝对式（不用累加，避免漂移）：
            double px0 = fromX + dx * t0, py0 = fromY + dy * t0, pz0 = fromZ + dz * t0;
            double px1 = fromX + dx * t1, py1 = fromY + dy * t1, pz1 = fromZ + dz * t1;
            int c0 = style.colorAt((float) t0, length * t0, timeSeconds);
            int c1 = style.colorAt((float) t1, length * t1, timeSeconds);
            double hw0 = halfWidth(width, px0, py0, pz0);
            double hw1 = halfWidth(width, px1, py1, pz1);
            ribbon(core, px0, py0, pz0, px1, py1, pz1, dirX, dirY, dirZ, hw0, hw1, c0, c1);
            if (glow != null) {
                // 辉光：同一条带加宽、压暗，走加法混合，看起来像能量在线里流动。
                ribbon(glow, px0, py0, pz0, px1, py1, pz1, dirX, dirY, dirZ,
                        halfWidth(glowWidth, px0, py0, pz0), halfWidth(glowWidth, px1, py1, pz1),
                        WireVisualColor.withAlpha(c0, 0.34F), WireVisualColor.withAlpha(c1, 0.34F));
            }
        }
    }

    /** 折线版线路（附属模组的能量电线 / 物流管道常常是多点路径）。 */
    public void wirePath(List<Vec3> points, WireVisualStyle style) {
        for (int i = 0; i + 1 < points.size(); i++) {
            wire(points.get(i), points.get(i + 1), style);
        }
    }

    /** 线框（12 条边）：端点标记与外框提示都用它。 */
    public void boxOutline(AABB box, WireVisualStyle style) {
        boxOutline(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, style);
    }

    /** 方块线框版：避免为每个端点构造临时 AABB。 */
    public void boxOutline(BlockPos pos, WireVisualStyle style) {
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();
        boxOutline(x, y, z, x + 1.0, y + 1.0, z + 1.0, style);
    }

    private void boxOutline(double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ, WireVisualStyle style) {
        cornersOf(minX, minY, minZ, maxX, maxY, maxZ);
        QuadBatch core = core(style.layer());
        float gw = style.glowScale();
        QuadBatch glow = (suppressGlow || gw <= 0.0F) ? null : glow(style.layer());
        int color = style.colorAt(0.5F, 0.0, timeSeconds);
        double width = style.width();
        int glowColor = WireVisualColor.withAlpha(color, 0.28F);
        double glowWidth = width * gw;
        for (int[] edge : EDGES) {
            int ia = edge[0] * 3, ib = edge[1] * 3;
            double ax = cornerScratch[ia], ay = cornerScratch[ia + 1], az = cornerScratch[ia + 2];
            double bx = cornerScratch[ib], by = cornerScratch[ib + 1], bz = cornerScratch[ib + 2];
            double dx = bx - ax, dy = by - ay, dz = bz - az;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0E-6) {
                continue;
            }
            double dirX = dx / len, dirY = dy / len, dirZ = dz / len;
            double hwA = halfWidth(width, ax, ay, az);
            double hwB = halfWidth(width, bx, by, bz);
            ribbon(core, ax, ay, az, bx, by, bz, dirX, dirY, dirZ,
                    hwA, hwB, color, color);
            if (glow != null) {
                ribbon(glow, ax, ay, az, bx, by, bz, dirX, dirY, dirZ,
                        halfWidth(glowWidth, ax, ay, az), halfWidth(glowWidth, bx, by, bz), glowColor, glowColor);
            }
        }
    }

    /**
     * 四角括号式选框：每个角只画三段短边，比整只立方体清爽，适合"准星命中"提示。
     *
     * @param armRatio 每条短臂占该方向边长的比例（0..0.5）
     */
    public void cornerBrackets(AABB box, WireVisualStyle style, float armRatio) {
        cornerBrackets(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, style, armRatio);
    }

    /** 方块四角括号式选框，不分配中间 AABB。 */
    public void cornerBrackets(BlockPos pos, WireVisualStyle style, float armRatio) {
        double x = pos.getX() - 0.004, y = pos.getY() - 0.004, z = pos.getZ() - 0.004;
        cornerBrackets(x, y, z, x + 1.008, y + 1.008, z + 1.008, style, armRatio);
    }

    private void cornerBrackets(double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ,
                                WireVisualStyle style, float armRatio) {
        cornersOf(minX, minY, minZ, maxX, maxY, maxZ);
        QuadBatch core = core(style.layer());
        float gw = style.glowScale();
        QuadBatch glow = (suppressGlow || gw <= 0.0F) ? null : glow(style.layer());
        int color = style.colorAt(0.5F, 0.0, timeSeconds);
        double width = style.width();
        int glowColor = WireVisualColor.withAlpha(color, 0.28F);
        double glowWidth = width * gw;
        double sizeX = maxX - minX, sizeY = maxY - minY, sizeZ = maxZ - minZ;
        for (int i = 0; i < 8; i++) {
            double cx = cornerScratch[i * 3], cy = cornerScratch[i * 3 + 1], cz = cornerScratch[i * 3 + 2];
            for (int axis = 0; axis < 3; axis++) {
                double size = axis == 0 ? sizeX : axis == 1 ? sizeY : sizeZ;
                double arm = Math.min(size * 0.45, Math.max(0.03, size * armRatio));
                // 臂朝盒子内部延伸：该角在此轴上取的是 min 就往正方向画，反之往负方向。
                double sign = ((i >> axis) & 1) == 0 ? 1.0 : -1.0;
                double dirX = axis == 0 ? sign : 0.0;
                double dirY = axis == 1 ? sign : 0.0;
                double dirZ = axis == 2 ? sign : 0.0;
                double ex = cx + dirX * arm, ey = cy + dirY * arm, ez = cz + dirZ * arm;
                double hw0 = halfWidth(width, cx, cy, cz);
                double hw1 = halfWidth(width, ex, ey, ez);
                ribbon(core, cx, cy, cz, ex, ey, ez, dirX, dirY, dirZ, hw0, hw1, color, color);
                if (glow != null) {
                    ribbon(glow, cx, cy, cz, ex, ey, ez, dirX, dirY, dirZ,
                            halfWidth(glowWidth, cx, cy, cz), halfWidth(glowWidth, ex, ey, ez), glowColor, glowColor);
                }
            }
        }
    }

    /** 半透明填充盒（6 个面），颜色自带透明度。 */
    public void boxFill(AABB box, int argb, WireVisualLayer layer) {
        boxFill(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, argb, layer);
    }

    /** 方块整体的半透明填充，不构造临时 AABB。 */
    public void boxFill(BlockPos pos, int argb, WireVisualLayer layer) {
        double x = pos.getX() - 0.004, y = pos.getY() - 0.004, z = pos.getZ() - 0.004;
        boxFill(x, y, z, x + 1.008, y + 1.008, z + 1.008, argb, layer);
    }

    private void boxFill(double minX, double minY, double minZ,
                         double maxX, double maxY, double maxZ, int argb, WireVisualLayer layer) {
        if (pointCulled((minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5)) {
            return;
        }
        cornersOf(minX, minY, minZ, maxX, maxY, maxZ);
        QuadBatch core = core(layer);
        for (int[] face : FACES) {
            int a = face[0] * 3, b = face[1] * 3, c = face[2] * 3, d = face[3] * 3;
            core.quad(
                    cornerScratch[a], cornerScratch[a + 1], cornerScratch[a + 2], argb,
                    cornerScratch[b], cornerScratch[b + 1], cornerScratch[b + 2], argb,
                    cornerScratch[c], cornerScratch[c + 1], cornerScratch[c + 2], argb,
                    cornerScratch[d], cornerScratch[d + 1], cornerScratch[d + 2], argb);
        }
    }

    /** 中心亮点：正对相机的小菱形，用来标出端点的确切位置。 */
    public void nodeDot(Vec3 center, float size, int argb, WireVisualLayer layer) {
        nodeDot(center.x, center.y, center.z, size, argb, layer);
    }

    private void nodeDot(double centerX, double centerY, double centerZ,
                         float size, int argb, WireVisualLayer layer) {
        if (pointCulled(centerX, centerY, centerZ)) {
            return;
        }
        double vx = centerX - camX, vy = centerY - camY, vz = centerZ - camZ;
        double dist = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (dist < 1.0E-4) {
            return;
        }
        double dirX = vx / dist, dirY = vy / dist, dirZ = vz / dist;
        // ref = |dir.y| > 0.98 ? X_AXIS : Y_AXIS; right = normalize(dir × ref); up = normalize(right × dir)
        double rx, ry, rz;
        if (Math.abs(dirY) > 0.98) {
            rx = 0.0; ry = dirZ; rz = -dirY; // dir × (1,0,0) = (0, z, -y)
        } else {
            rx = -dirZ; ry = 0.0; rz = dirX; // dir × (0,1,0) = (-z, 0, x)
        }
        double rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rl < NORMALIZE_EPS) {
            return;
        }
        rx /= rl; ry /= rl; rz /= rl;
        double ux = ry * dirZ - rz * dirY, uy = rz * dirX - rx * dirZ, uz = rx * dirY - ry * dirX;
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul < NORMALIZE_EPS) {
            return;
        }
        ux /= ul; uy /= ul; uz /= ul;
        double half = halfWidth(size, centerX, centerY, centerZ);
        QuadBatch glow = glow(layer);
        if (glow != null) {
            int ring = WireVisualColor.withAlpha(argb, 0.6F);
            glow.quad(
                    centerX + rx * half - ux * half, centerY + ry * half - uy * half, centerZ + rz * half - uz * half, ring,
                    centerX + rx * half + ux * half, centerY + ry * half + uy * half, centerZ + rz * half + uz * half, ring,
                    centerX + rx * -half + ux * half, centerY + ry * -half + uy * half, centerZ + rz * -half + uz * half, ring,
                    centerX + rx * -half - ux * half, centerY + ry * -half - uy * half, centerZ + rz * -half - uz * half, ring);
        }
        double inner = half * 0.45;
        QuadBatch core = core(layer);
        int bright = WireVisualColor.brighten(argb, 1.3F);
        core.quad(
                centerX + rx * inner - ux * inner, centerY + ry * inner - uy * inner, centerZ + rz * inner - uz * inner, bright,
                centerX + rx * inner + ux * inner, centerY + ry * inner + uy * inner, centerZ + rz * inner + uz * inner, bright,
                centerX + rx * -inner + ux * inner, centerY + ry * -inner + uy * inner, centerZ + rz * -inner + uz * inner, bright,
                centerX + rx * -inner - ux * inner, centerY + ry * -inner - uy * inner, centerZ + rz * -inner - uz * inner, bright);
    }

    /** 线路端点：单位立方体线框 + 淡填充 + 中心亮点，一次画完。 */
    public void endpoint(Vec3 center, WireVisualStyle style, float breathe) {
        endpoint(center.x - 0.5, center.y - 0.5, center.z - 0.5,
                center.x + 0.5, center.y + 0.5, center.z + 0.5,
                center.x, center.y, center.z, style, breathe);
    }

    /** 线路端点（自定义包围盒，附属模组的 2×2 机器或多格端口用这个）。 */
    public void endpoint(AABB box, Vec3 center, WireVisualStyle style, float breathe) {
        endpoint(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                center.x, center.y, center.z, style, breathe);
    }

    /** 线路端点（方块坐标版），不创建临时 AABB / Vec3。 */
    public void endpoint(BlockPos pos, WireVisualStyle style, float breathe) {
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();
        endpoint(x, y, z, x + 1.0, y + 1.0, z + 1.0,
                x + 0.5, y + 0.5, z + 0.5, style, breathe);
    }

    private void endpoint(double minX, double minY, double minZ,
                          double maxX, double maxY, double maxZ,
                          double centerX, double centerY, double centerZ,
                          WireVisualStyle style, float breathe) {
        boxOutline(minX, minY, minZ, maxX, maxY, maxZ, style);
        int base = style.colorAt(0.5F, 0.0, timeSeconds);
        boxFill(minX, minY, minZ, maxX, maxY, maxZ,
                WireVisualColor.withAlpha(base, 0.20F + 0.16F * breathe), style.layer());
        nodeDot(centerX, centerY, centerZ, 0.34F + 0.14F * breathe,
                WireVisualColor.brighten(base, 1.0F + 0.4F * breathe), style.layer());
    }

    /** 某个方块的本帧包围盒（可整体膨胀，让描边不与已有几何抢深度）。 */
    public static AABB blockBox(BlockPos pos, double inflate) {
        return new AABB(pos.getX() - inflate, pos.getY() - inflate, pos.getZ() - inflate,
                pos.getX() + 1.0 + inflate, pos.getY() + 1.0 + inflate, pos.getZ() + 1.0 + inflate);
    }

    // ---------------------------------------------------------------- 内部几何工具

    private boolean pointCulled(double x, double y, double z) {
        double vx = x - camX, vy = y - camY, vz = z - camZ;
        return vx * vx + vy * vy + vz * vz > cullDistanceSq;
    }

    /** 整条线段都太远时才丢弃：两端都远、中段离相机很近的情况仍然要画。 */
    private boolean segmentCulled(double ax, double ay, double az, double bx, double by, double bz,
                                  double abx, double aby, double abz) {
        double lenSq = abx * abx + aby * aby + abz * abz;
        if (lenSq < 1.0E-8) {
            return pointCulled(ax, ay, az);
        }
        // t = clamp( (camera - a) · ab / |ab|², 0, 1 )；closest = a + ab·t
        double vx = camX - ax, vy = camY - ay, vz = camZ - az;
        double t = Math.max(0.0, Math.min(1.0, (vx * abx + vy * aby + vz * abz) / lenSq));
        double cx = ax + abx * t, cy = ay + aby * t, cz = az + abz * t;
        return pointCulled(cx, cy, cz);
    }

    /** 距离补偿后的半宽：近处等于真实世界宽度，远处按屏幕等宽放大。 */
    private double halfWidth(double width, double x, double y, double z) {
        double vx = x - camX, vy = y - camY, vz = z - camZ;
        double dist = Math.sqrt(vx * vx + vy * vy + vz * vz);
        double scale = Math.min(MAX_WIDTH_SCALE, Math.max(1.0, dist / REFERENCE_DISTANCE));
        return width * 0.5 * scale;
    }

    /** 把一条小段展成面向相机的四边形（标量版，零分配）。 */
    private void ribbon(QuadBatch target,
                        double p0x, double p0y, double p0z, double p1x, double p1y, double p1z,
                        double dirX, double dirY, double dirZ,
                        double hw0, double hw1, int color0, int color1) {
        if (target == null || (pointCulled(p0x, p0y, p0z) && pointCulled(p1x, p1y, p1z))) {
            return;
        }
        sideAt(p0x, p0y, p0z, dirX, dirY, dirZ, sideScratch0);
        sideAt(p1x, p1y, p1z, dirX, dirY, dirZ, sideScratch1);
        double[] s0 = sideScratch0, s1 = sideScratch1;
        target.quad(
                p0x + s0[0] * hw0, p0y + s0[1] * hw0, p0z + s0[2] * hw0, color0,
                p0x - s0[0] * hw0, p0y - s0[1] * hw0, p0z - s0[2] * hw0, color0,
                p1x - s1[0] * hw1, p1y - s1[1] * hw1, p1z - s1[2] * hw1, color1,
                p1x + s1[0] * hw1, p1y + s1[1] * hw1, p1z + s1[2] * hw1, color1);
    }

    /** 线段在某个点上的"屏幕水平方向"：段方向与视线方向的叉乘（标量版，写入 out[3]）。 */
    private void sideAt(double px, double py, double pz,
                        double dirX, double dirY, double dirZ, double[] out) {
        double viewX = px - camX, viewY = py - camY, viewZ = pz - camZ;
        double sx = dirY * viewZ - dirZ * viewY;
        double sy = dirZ * viewX - dirX * viewZ;
        double sz = dirX * viewY - dirY * viewX;
        if (sx * sx + sy * sy + sz * sz < 1.0E-8) {
            // 线段正好指向相机：退回任意一个与段方向垂直的方向（ref = |dir.y| > 0.98 ? X : Y）。
            if (Math.abs(dirY) > 0.98) {
                sx = 0.0; sy = dirZ; sz = -dirY; // dir × (1,0,0)
            } else {
                sx = -dirZ; sy = 0.0; sz = dirX; // dir × (0,1,0)
            }
        }
        double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < NORMALIZE_EPS) {
            out[0] = 0.0; out[1] = 0.0; out[2] = 0.0; // 复刻 Vec3.normalize() 的退化分支
            return;
        }
        out[0] = sx / len; out[1] = sy / len; out[2] = sz / len;
    }

    /** 把盒子的 8 个角写进暂存数组（顺序与 {@link #EDGES}/{@link #FACES} 一致）。 */
    private void cornersOf(AABB box) {
        cornersOf(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    private void cornersOf(double minX, double minY, double minZ,
                           double maxX, double maxY, double maxZ) {
        double[] c = cornerScratch;
        c[0] = minX; c[1] = minY; c[2] = minZ;
        c[3] = maxX; c[4] = minY; c[5] = minZ;
        c[6] = minX; c[7] = maxY; c[8] = minZ;
        c[9] = maxX; c[10] = maxY; c[11] = minZ;
        c[12] = minX; c[13] = minY; c[14] = maxZ;
        c[15] = maxX; c[16] = minY; c[17] = maxZ;
        c[18] = minX; c[19] = maxY; c[20] = maxZ;
        c[21] = maxX; c[22] = maxY; c[23] = maxZ;
    }

    /** 立方体 12 条边（顶点索引对上，索引见 {@link #cornersOf}）。 */
    private static final int[][] EDGES = {
            {0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    /** 立方体 6 个面（绕序无所谓——本模块的自定义管线一律关闭背面剔除）。 */
    private static final int[][] FACES = {
            {0, 1, 3, 2}, {4, 6, 7, 5}, {0, 2, 6, 4}, {1, 5, 7, 3}, {0, 4, 5, 1}, {2, 3, 7, 6}
    };

    /**
     * 一组四边形顶点：相机相对坐标 + ARGB 颜色；提交时以 QUADS 模式画，每 4 个顶点一个面。
     *
     * <p>坐标在写入时就把 double 相机偏移算掉，避免远距离世界坐标转 float 丢精度。
     */
    public final class QuadBatch {
        private float[] xyz = new float[192];
        private int[] argb = new int[64];
        private int count;

        void clear() {
            count = 0;
        }

        public int vertexCount() {
            return count;
        }

        public boolean isEmpty() {
            return count == 0;
        }

        /** 写一个世界坐标顶点（内部换算成相机相对坐标）。 */
        void vertex(double worldX, double worldY, double worldZ, int color) {
            ensure(count + 1);
            int i = count * 3;
            xyz[i] = (float) (worldX - camX);
            xyz[i + 1] = (float) (worldY - camY);
            xyz[i + 2] = (float) (worldZ - camZ);
            argb[count] = color;
            count++;
        }

        /** 写一个世界坐标顶点（Vec3 版，附属模组手写几何时的便捷入口）。 */
        public void vertex(Vec3 world, int color) {
            vertex(world.x, world.y, world.z, color);
        }

        /** 一个面，四个角共用一个颜色。 */
        public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
            vertex(a.x, a.y, a.z, color);
            vertex(b.x, b.y, b.z, color);
            vertex(c.x, c.y, c.z, color);
            vertex(d.x, d.y, d.z, color);
        }

        /** 一个面，沿"起点色 → 终点色"渐变（a、b 在起点侧，c、d 在终点侧）。 */
        public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int startColor, int endColor) {
            vertex(a.x, a.y, a.z, startColor);
            vertex(b.x, b.y, b.z, startColor);
            vertex(c.x, c.y, c.z, endColor);
            vertex(d.x, d.y, d.z, endColor);
        }

        /** 标量版一个面（热路径专用，零分配）。 */
        void quad(double ax, double ay, double az, int ac,
                  double bx, double by, double bz, int bc,
                  double cx, double cy, double cz, int cc,
                  double dx, double dy, double dz, int dc) {
            ensure(count + 4);
            vertex(ax, ay, az, ac);
            vertex(bx, by, bz, bc);
            vertex(cx, cy, cz, cc);
            vertex(dx, dy, dz, dc);
        }

        private void ensure(int vertices) {
            if (vertices * 3 <= xyz.length) {
                return;
            }
            int capacity = Math.max(vertices * 3, xyz.length * 2);
            float[] grown = new float[capacity];
            System.arraycopy(xyz, 0, grown, 0, count * 3);
            xyz = grown;
            int[] colors = new int[capacity / 3];
            System.arraycopy(argb, 0, colors, 0, count);
            argb = colors;
        }

        /** 在自定义几何提交回调里把顶点写进渲染缓冲。 */
        public void writeTo(PoseStack.Pose pose, VertexConsumer buffer) {
            for (int i = 0; i < count; i++) {
                int color = argb[i];
                buffer.addVertex(pose, xyz[i * 3], xyz[i * 3 + 1], xyz[i * 3 + 2])
                        .setColor(WireVisualColor.red(color), WireVisualColor.green(color),
                                WireVisualColor.blue(color), WireVisualColor.alpha(color));
            }
        }
    }
}
