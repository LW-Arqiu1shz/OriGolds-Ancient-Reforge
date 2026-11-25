# 2026-11-10 整理说明（以本节为准）

源码已从两个 zip 拆成三套并排的独立 Gradle 工程，放在 `源码\` 下：
`1.21.1NeoForge`（Java 21）、`26.1.2NeoForge`、`26.3NeoForge`（Java 25，ModDev 2.0.148）。每套各自 `gradle build`，
jar 在各自的 `build\libs\`，文件名带 `-rN`、mods.toml 里的版本号不带。下文里 `/`（仓库根）、`mc26.3/` 是旧平台的布局，已不再使用。

1.3.0（1.21.1 r205 / 26.x r34；r203 / r32 是第一版）：
- r205 / r34：行囊改成能放的方块 `otherworld_purse`（`purse/OtherworldPurseBlock` + `OtherworldPurseBlockEntity`，物品改继承 BlockItem）。
  只有 Shift+右键才放（`useOn` 不按 Shift 返回 PASS → 走 `use` 开界面）；方块实体存整个行囊 ItemStack，挖掉 / 被移除时原样掉出
  （1.21.1 `onRemove`，26.x `preRemoveSideEffects`；不看 doTileDrops）；右键地上的行囊开同一个 3×3 界面（`writePlaced`，没有装备按钮），
  多人同时打开共用方块实体里的 `StackContainer`。大小、碰撞箱、手持显示和沉重核心一样（4..12 × 0..8）；硬度 0.33（空手 10 刻）、
  抗爆 3600000、活塞推不动（26.3 叫 IMMOVEABLE）、进 dragon_immune / wither_immune。贴图改成近乎全黑（≤2% 渐变），
  挪到 `textures/block/`，顶面菱形帧时 4 刻。掉在地上的行囊本来就 invulnerable，爆炸伤不到。
- r204 / r33 按用户试玩反馈改：贴图换成末地折跃门样子（`otherworld_purse_side` 静止星空，`otherworld_purse_top` 加 56 帧变色菱形，每帧 2 刻）；
  展开的一行用原版 `inventory.png` 拼、接在物品栏顶上（ROW_Y -20）；图标底下不画格子；创造模式背包页图标在 (127,20)（副手格的对称位置），
  右键 `ACTION_TAKE_TO_INVENTORY` 放回物品栏（创造模式鼠标物品归客户端，不能用 setCarried）；
  鼠标键改用 `InputConstants.MOUSE_BUTTON_*`（26.3 左键是 1、右键是 3）；格子收起时 mayPlace / mayPickup 都 false（防双击收集吸走行囊里的东西）；
  行囊内容算物品栏延伸：`PurseData.contents / withInventory`，勋章、符文、复苏图腾、虚空箭袋的判定都用它；
  扔出：有主人时速度 = 视线方向 × 0.35，每刻再 ×0.92 阻力，约 3.6 格停下；装备按钮挪到界面正上方。
- 新物品异界行囊 `otherworld_purse`（代码在 `purse\` 包）。数据：玩家附件 `origolds_ar:otherworld_purse`（`PurseData`，copyOnDeath），
  装备时行囊本身 + 9 格物品分开存，取下时物品写回行囊的 `minecraft:container` 组件。
- 物品栏菜单加 9 个格子：`mixin/InventoryMenuPurseMixin`（构造器 RETURN 处 addSlot，排在原有格子后面，索引 46–54）+ `MenuInvoker`。
  格子只在装备 + 展开 + 当前界面是 InventoryScreen 时显示（创造模式物品栏里不显示）；展开时 Shift 点击背包物品优先进行囊。
- 客户端 `client/PurseInventoryOverlay`：图标在 (77,44)，装了 travelersbackpack 时上移到 (77,26)；面板画在界面顶边上方。
  1.21.1 用 ContainerScreenEvent.Render.Background/Foreground；26.x 用 ScreenEvent.Render.Background/Foreground。
- 死亡：LivingDeathEvent(LOWEST) 收走物品栏里的行囊存进附件 kept，PlayerRespawnEvent 还回去（keepInventory 开着时不动）。
- 掉落物：EntityJoinLevelEvent 设无重力 / 无敌 / 永不消失；EntityLeaveLevelEvent 里 removalReason.shouldDestroy() 时把内容物撒出来。
- 26.x API 差异：ValueIOSerializable、ContainerInput（原 ClickType）、`canFitInsideContainerItems(ItemStack)`；
  26.3 另有 `gui.screen()`、`getLeftPos/getTopPos/getHoveredSlot`、`setPermanentlyInvulnerable`、`drop(stack, false, Prediction)`。
- 刺客勋章配方：幻翼膜 → 火药。自循环附魔之瓶补上提示；`appendHints` 的实用提示支持 `\n` 分行。

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
