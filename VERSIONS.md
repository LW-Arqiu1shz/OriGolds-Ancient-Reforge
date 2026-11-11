# 2026-11-10 整理说明（以本节为准）

源码已从两个 zip 拆成三套并排的独立 Gradle 工程，放在 `源码\` 下：
`1.21.1NeoForge`（Java 21）、`26.1.2NeoForge`、`26.3NeoForge`（Java 25，ModDev 2.0.148）。每套各自 `gradle build`，
jar 在各自的 `build\libs\`，文件名带 `-rN`、mods.toml 里的版本号不带。下文里 `/`（仓库根）、`mc26.3/` 是旧平台的布局，已不再使用。

1.2.2（1.21.1 r202 / 26.x r31）：
- 模组列表双语适配（三版）：名称按 `fml.menu.mods.info.displayname.origolds_ar` 翻译，搜索同时匹配中英文名，简介按语言显示。
  1.21.1 / 26.1.2：`ArModListNames` + 客户端 Mixin（ModListScreen / ModListEntry / ModListSort，与工业时代同一套）。
  26.3：官方 `ModDisplayInfo` 扩展点（`client.ArModDisplayInfo`）+ 搜索用 `ModListSearchMixin`；简介键是 `neoforge.screen.mods.info.description.origolds_ar`。
  针对编译器生成 lambda 的注入都是 require = 0（NeoForge 改了编号只会少掉双名搜索，不会崩）。
- 1.21.1：新增客户端配置 `config/origolds_ar-client.toml` 的 `wireStyle`（CLASSIC / NEO，默认 NEO），也可在模组列表的“配置”按钮里改，即时生效。
  NEO = 与 26.x 一致的发光线带样式：`api/visual` 的画布 / 样式 / 通道代码原样移植到 1.21.1，只有 `WireVisuals` 换成 1.21.1 的即时模式绘制（AFTER_WEATHER，四批：辉光 / 核心 × 遮挡 / 穿墙）。
  CLASSIC = 原来的 1.21.1 细线样式（`WirelessConnectorClient#onRenderLevel`）。
- 复苏图腾两版行为差异（1.21.1 清全部效果 / 26.x 只清负面）按用户决定保持不变。
1.2.1（1.21.1 r200 / 26.x r29）：
- 崩溃：发射器射出的箭（没有记录武器）打中任何生物时，词条穿透逻辑对 null 调 `isEmpty()`，整个服务器崩溃（三版）。
- 崩溃：被改坏的词条数据（空数组 / 越界 id）会让悬停提示（客户端）或碎片合成结果（服务端）越界崩溃（三版）。
- 卡顿：无线红石线路的源/终点在已加载区域边缘时，读邻居信号与通知邻居会每 tick 同步加载未加载区块；现在跳过、下 tick 再试（三版）。
- 钩爪：换维度 / 重生时清除绑定与拖拽，不再按旧维度坐标把人拖过大片未加载地形（三版）。
- 贪婪符文：补掷数量超过一组时拆组掉落（超大物品堆无法存盘）；每张战利品表只序列化一次（三版）。
- 结构方块摆放：包围盒只算 8 个角，不再逐格变换整块结构（三版）。
- 词条查询：没有词条的物品不再整份复制自定义数据（三版）。
- 钩爪路径预览：目标不变时不再每 tick 发包（三版）。
- 26.x：无线红石存档去掉错误的 `DataFixTypes.LEVEL`；坏 UUID 不再导致整份线路数据丢失。
- 26.x：新增 `WirelessLegacyMigration`，开服时导入 1.21.1 的 `origolds_ar_wireless_redstone.dat`，导入后改名 `*.migrated`。
- 26.x：补回移植时漏掉的 `LunarGravityMixin`（月球重力终端速度 40%，挂在 `travelInAir` 的 0.98F 上）。
- 26.x：饮用型永恒药水喝不了（26.x 的 PotionItem 不再覆写 use），改为直接开始饮用。
- 26.x：钩爪弩炮装填烟花时也显示已装填模型；结构文件里 1.21.1 格式的遥控开关 UUID 也能读。
# 双版本源码布局（26.1.2 / 26.3）

同一个模组的两个并行目标，源码与产物完全隔离，互不覆盖：

| 目标版本 | 工程根 | 源码 | 产物 jar |
| --- | --- | --- | --- |
| Minecraft 26.1.2 / NeoForge 26.1.2.103 | `/`（仓库根） | `src/main/...` | `build/libs/origolds_ar-1.2.1+26.1.2NeoForge.jar` |
| Minecraft 26.3 / NeoForge 26.3.0.51-beta | `mc26.3/`（独立 Gradle 工程） | `mc26.3/src/main/...` | `mc26.3/build/libs/origolds_ar-1.2.1+26.3NeoForge.jar` |

两边 `settings.gradle` 的 `rootProject.name` 都是 `origolds_ar`，是**两个互不包含的工程**：
改任何一边都不要顺手同步到另一边，除非那处改动与版本 API 无关（例如纯逻辑修复、贴图、文案）。

## 26.3 相对 26.1.2 的移植差异（源码里都有对应注释）

- 方块不再有 `MapCodec`：`simpleCodec` / `codec()` 已删除（`AssemblyStationBlock`）。
- 管线构建：`withVertexFormat(format, Mode)` → `withVertexBinding(0, format)` +
  `withPrimitiveTopology(PrimitiveTopology.QUADS)`（`WireVisuals`）。
- `RenderSetup` 不再有输出目标与缓冲大小：删掉 `setOutputTarget(MAIN_TARGET)` 与
  `bufferSize(...)`，主画面输出与 `LayeringTransform.NO_LAYERING` 是默认行为（`WireVisuals`）。
- `submitNameTag` 去掉 `distanceToCameraSq` 参数，末尾改传 `CameraRenderState`（`VolunteerDummyRenderer`）。
- `BlockState#blocksMotion()` 删除，等价判定用 `getCollisionShape(level, pos).isEmpty()`（`RecoveryCompassEvents`）。
- `EntityType.createDefaultStackConfig` 返回 `PostSpawnProcessor<T>` 而不是 `Consumer<T>`（`VolunteerDummyItem`）。
- `pack.mcmeta` 的 `max_format` 提到 121（26.3 的数据包格式主版本号）。

## 运行时前置

两版 jar 都要求前置模组 `origolds`（金源工艺本体）已安装，版本范围 `[1.0.0,)`；
26.3 版还需 NeoForge `[26.3.0.51-beta,)`。附属 `origolds_ia` 用的是 `WireVisuals` 公开 API，
签名两版一致，但附属自己也得针对目标版本重编一次。
