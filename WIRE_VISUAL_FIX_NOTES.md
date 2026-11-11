# OGAR 无线线路渲染修复说明

目标环境：Minecraft 26.1.2 / NeoForge 26.1.2。

## 修改内容

### 1. 无线线路穿墙显示
- `src/main/java/cn/blockforge/generated/origoldsar/client/WirelessConnectorClient.java`
  - `WIRE_LINE` 显式指定 `WireVisualLayer.OVERLAY`，使无线红石线路不依赖样式工厂的默认层级。
- `src/main/java/cn/blockforge/generated/origoldsar/api/visual/WireVisuals.java`
  - OVERLAY 的核心与辉光管线使用 `CompareOp.ALWAYS_PASS`，不进行深度遮挡测试。
  - OVERLAY 几何通过 `SubmitNodeCollector.order(1)` 延后到常规世界几何之后提交，避免后续世界几何再次覆盖线路；普通遮挡层仍留在 `order(0)`。
  - 深度写入保持关闭，深度偏移明确设为 0。

### 2. Iris 光影兼容
- `WireVisuals.registerPipelines` 在注册四条自定义管线后，尝试调用 Iris 的公开 API。
- 将四条 OGAR 自有管线分别映射至 `IrisProgram.BASIC`，使 Iris 使用光影包的 basic 程序处理 `POSITION_COLOR` 几何。
- 全部通过反射调用；未安装 Iris 时直接跳过，不添加硬依赖。若 Iris API 不兼容，会记录警告，不阻止无光影环境运行。

## 验证状态

已完成源码级静态检查：四条管线均仍被注册；两个穿墙管线均使用 `ALWAYS_PASS`；深度写入关闭；四条管线均加入可选 Iris 映射；源码没有对 Iris 的编译期依赖。

尚未进行 Gradle 编译和游戏内验证。当前工作环境没有 Gradle、项目没有 Gradle Wrapper，且只有 Java 21，而项目要求 Java 25。因此不能声称已编译成功或已在游戏中验证。请在配置好 Java 25 和 Gradle 的开发环境中构建，并分别测试：无光影穿墙、Iris 1.11.x 开启任意光影后的显示、切换光影包/重载资源后是否仍显示。
