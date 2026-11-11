# OGAR 光影下帧率回退调查报告（26.1.2 移植）

调查目标：1.21.1 开 Iris 稳定 120 FPS，26.1.2 移植版开 Iris 只剩 ~67 FPS；关光影两版都正常。
本报告给出：①两版客户端渲染的逐项对比；②Iris 侧机制的源码证据；③可疑文件与调用链；④本轮实施的最小修复；⑤需要你配合完成的实测步骤。

## 1. 两版客户端渲染逐项对比

对照组：官方 1.21.1 发布 jar（`origolds_ar-1.2.0_1.21.1NeoForge.jar`，反汇编核对）vs 本工程源码。

除 `api/visual/*`、`client/WirelessConnectorClient` 外，两版其余客户端类（HUD、实体渲染器、
键位、界面）逐文件比对**无新增逐帧渲染路径**；5 个 mixin 均不触碰渲染线程热路径。
因此客户端渲染的移植差异**全部集中在无线视觉系统**：

| 项目 | 1.21.1（正常基线） | 26.1.2 移植（回退点） |
| --- | --- | --- |
| 触发事件 | `RenderLevelStageEvent`(AFTER_WEATHER) | `SubmitCustomGeometryEvent`（提交节点模型，26.1 新架构） |
| 绘制方式 | `Tesselator` + `BufferUploader.drawWithShader` 即时模式 | `submitCustomGeometry` 自定义几何提交 |
| 着色程序 | 原版 position_color（Iris 已有托管映射） | **自建 4 条 RenderPipeline**，反射 `assignPipeline(→BASIC)` |
| 线体几何 | GL 线段：每段 2 顶点、1px、无填充成本 | 相机 facing 四边形带：每 2 格 1 段（上限 160 段），核心 + 辉光两条 |
| 每帧 draw 数 | ≤2 | ≤4（遮挡桶 order(0) + 穿墙桶 order(1)） |
| 逐帧顶点生成 | 每帧 2 顶点/线 | **每帧全量重建**：每段约 12 次 `Vec3` 分配（改造前） |
| 剔除 | 相机距离 | 相机距离 192 格（**无视锥剔除**，改造前） |

附属 origolds_ia 的金源能量电线/物流管道**共用同一套 `WireVisuals` 通道与管线**，
它注册一路常开通道即可画线——所以本文件里说的回退，两个模组一起承担；本轮的优化两边自动继承。

## 2. Iris 侧机制与证据（源码级，非猜测）

- Iris 26.1 对自定义管线走 `IrisPipelines.assignPipeline → ShaderKey.findBestMatch`；我们的管线是
  POSITION_COLOR，命中的是 `BASIC_COLOR`（ProgramId.Basic + POSITION_COLOR，雾 OFF、LIGHTMAP 照明）——
  顶点格式匹配正确，**不存在格式仿真回退**。程序本体在光影加载时编译一次、`ShaderMap` 数组缓存，
  无每帧重编译。
- 每次渲染段绑定该程序时执行 `ExtendedShader.iris$setupState`：`glUseProgram`、modelView/projection
  **两次矩阵求逆**、法向矩阵、全部采样器/Uniform/自定义 Uniform 上传、并重新 bind Iris 的
  gbuffer FBO（`writingToBefore/AfterTranslucent`）。每帧按"程序数 × 渲染段数"计。
- 对带 custom pass 的渲染段，`MixinGlCommandEncoder` 强制 `lastProgram = null` → 该段内**每次 draw
  都重新上传全部 uniform**。
- 结论：在 Iris 下我们每帧的 4 次提交比 vanilla 下贵得多；而**每条线的几何量比 1.21.1 大了
  两个数量级**（GL 线 2 顶点 → 最多 320 个带填充的四边形/线，全部每帧重建）。
  Iris 官方 issue [#2940](https://github.com/IrisShaders/Iris/issues/2940) 是同型先例：
  单个绘制调用的基元/宽度不匹配光影预期，实测一个调用 9ms、帧率直接掉到 30。

## 3. 可疑文件与调用链（按优先级）

1. `api/visual/WireVisualCanvas.java` —— 每帧全量重建 + 逐段对象分配 + 无视野剔除（CPU + GC，
   附属的线网规模下放大）。
2. `api/visual/WireVisuals.java` —— 每帧最多 4 次 `submitCustomGeometry`（4 个独立 buffer 上传、
   4 次管线绑定，其中 order(1) 是独立晚提交桶）。
3. `client/WirelessConnectorClient.java` —— 每帧一次 32 格 `level.clip` 方块射线（仅手持时；纯 CPU，
   与光影无关，未改）。
4. 附属 origolds_ia 的通道内容（另行调查中，共用 1、2 两条链）。

## 4. 本轮已实施的最小修复（外观零改动）

- **视锥剔除**（`WireVisualCanvas` + `WireVisuals.beginFrame(cam, t, cullFrustum)`）：视野外的一条线
  连一个顶点都不再生成、不再提交——画在屏幕外的半透明四边形在光影下同样要付绑定与上传成本。
  **（已回退，见第 8 节：判定用错了相机，会把视野里的线路整条删掉。）**
- **热路径零分配重写**：`wire/boxOutline/cornerBrackets/nodeDot/ribbon/sideAt` 全部改为 double 标量 +
  复用暂存；逐段公式与旧实现逐位一致（绝对式取点，无累加漂移），输出几何不变。
- **order 桶惰性创建**：某层没有几何就不再调用 `collector.order(n)`，不再产生空的提交桶。
- **内置性能二分开关**（JVM 参数，默认不启用＝正常渲染）：
  `-Dorigolds_ar.wire_visual=off|glow_off|occluded_only`；
  `-Dorigolds_ar.wire_visual_stats=true` 每 120 帧在 `latest.log` 输出一行
  「几何收集 µs/帧 + 顶点数/帧 + 通道数」（CPU 部分直接量化，GPU 部分用开关二分）。
- Iris 映射与四条管线本身未动（显示行为维持现状）。

## 5. 需要你做的实测（这是唯一能分离 CPU/GPU 的手段）

同一存档、同一光影包、同一分辨率，依次各跑 30 秒记录 FPS 区间（F3 左侧）：

1. 现状（不加任何参数）；
2. 加 `-Dorigolds_ar.wire_visual=stats`，把手持连接装置、线路显示打开，跑一段后把
   `logs/latest.log` 里 `[WireVisuals]` 行发回来（看 CPU 与顶点规模）；
3. 加 `-Dorigolds_ar.wire_visual=off`——**回到 ~120 说明开销确实在无线视觉提交链**；
   仍 67 则问题在本体之外（附属自身逻辑或光影组合，报告里注明）；
4. 加 `-Dorigolds_ar.wire_visual=glow_off`——与 1 的差值 ≈ 辉光填充的 GPU 成本；
5. 加 `-Dorigolds_ar.wire_visual=occluded_only`——与 1 的差值 ≈ ALWAYS_PASS 晚提交桶的成本。

把 3/4/5 的数字和 stats 行发回来，下一轮就能对症做真正的削减（例如辉光合批、提交合并或
改用光影友好度更高的管线映射），而不是继续盲调。

## 6. 诚实边界

本容器无 GPU、无法启动客户端，**无法直接跑 GPU profiler**；上面的机制分析全部来自
两版二进制/源码逐项对比与 Iris 26.1 分支公开源码（issue #2940 已证实同型回退真实存在），
量化归因依赖第 5 节的 A/B 实测。


## 7. 本轮额外优化（源码静态检查）

- `WireVisualCanvas.wire(BlockPos, BlockPos, ...)` 直接从坐标标量写线体，避免每条链路每帧创建两个中心 `Vec3`；
  线路悬停采样也增加了 `BlockPos` 标量重载，避免每个采样点产生临时 `Vec3`。
- 线体细分从每 2 格放宽为每 4 格，减少常见长度线路约一半的带状四边形；线色、流光、穿墙层与核心宽度保持。
- OGAR 自己的主线辉光从 2.4× 收窄至 1.2×，待选端点从 1.8× 收窄至 0.9×，开关框从 1.6× 收窄至 0.8×。辉光仍保留，降低透明/加色层的覆盖面积。

这些改动针对源码中明确存在的顶点生成与辉光 overdraw 开销；真实 GPU 收益仍需在目标机器 + Iris 光影包下做 A/B 测试，不能仅凭静态检查承诺某个 FPS 数字。

## 8. 回退视锥剔除（本轮）

第 4 节加的视锥剔除会导致**无线线路整条不渲染**，本版已移除。原因：

- 本模块的几何取自 `LevelRenderState#cameraRenderState` 的相机（位置 + `lookOf()` 的朝向），
  剔除却拿 `cameraRenderState.cullFrustum` 去判——那个 `Frustum` 是方块/区块可见性流程按**另一套
  相机原点与另一个时刻**（外加 `offsetToFullyIncludeCameraCube` 的膨胀）准备的。自定义几何提交通道
  跑到我们这里时，两者并不同步。
- 后果就是误判：`frustum.isVisible(box)` 返回 false，一条**明明在视野里**的线一个顶点都不生成。
  线越长、跨屏越宽（包围盒越大反而更容易整盒被判在某个平面外）、观察者转动视角时越容易整条闪没，
  所以现象是"很多时候完全不渲染"，而不是"边缘缺一块"。
- 附带影响：`wire` 的 4 格细分与 `ribbon` 的距离判定里，靠近相机的中段仍会被画，但整条线的
  可见性由那次错误的盒判定决定——这就是"要么全有要么全无"的直接来源。

改动（外观与线宽不变）：

| 文件 | 改动 |
| --- | --- |
| [WireVisualCanvas.java](src/main/java/cn/blockforge/generated/origoldsar/api/visual/WireVisualCanvas.java) | 删除 `frustum` 字段与 `scratchBox`；删除 `frustumCulledSegment` / `frustumCulledBox`；`wire` / `boxOutline` / `cornerBrackets` / `boxFill` 不再做视锥判定；`beginFrame(Vec3, double)` 去掉视锥参数 |
| [WireVisuals.java](src/main/java/cn/blockforge/generated/origoldsar/api/visual/WireVisuals.java) | 不再把 `camera.cullFrustum` 传给画布 |

保留的剔除仍然只有**相机距离**（`CULL_DISTANCE = 192` 格）：逐段用 `pointCulled` / `segmentCulled`
标量判定，不依赖任何外部相机矩阵，不会漏画视野内的几何——这与 1.21.1 基线的行为一致。

性能上的取舍要说明白：视野外的线现在**又会生成顶点并参与提交**，第 4 节那部分 GPU 收益没了；
零分配标量热路径、4 格细分、order 桶惰性创建这些 CPU 侧优化不受影响。如果之后还要压 GPU，
正确的做法是用 `cameraRenderState` 自己的位置 + `lookOf()` 朝向现算六个平面（本模块内部构造、
保证同一相机同一时刻），而不是借用 `cullFrustum`；先用 `-Dorigolds_ar.wire_visual=glow_off`
与 `=off` 二分开关确认还剩多少开销在辉光与提交链上。
