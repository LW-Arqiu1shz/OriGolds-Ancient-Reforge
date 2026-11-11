# 金源工艺：古金极锻 · OriGolds: Ancient Reforge

> 本分支 / This branch: **26.3NeoForge**

[中文](#中文) · [English](#english)

> Author / 作者：**Arqiu-owo** · Code: `LGPL-3.0-only` · Assets: `CC-BY-NC-SA-4.0` — see [LICENSE.md](LICENSE.md)

---

## 中文

为《金源工艺》添加了后期玩法和一大堆实（超）用（标）的小道具。体验随机数的乐，感受道具赛的爽！

本模组是《[金源工艺](https://github.com/LW-Arqiu1shz/OriGolds)》（OriGolds）的附属模组，**需要先安装金源工艺**；客户端和服务端都要安装。

### 功能

- **赋源系统**
  - 破损金源部件放进**金源装配站**，从三个候选词条里选一个，得到带三条随机词条的赋源金源部件。
  - 词条共 7 种：攻击、暴伤、耐久、工具效率、穿甲、护甲值、盔甲韧性，每条有白 / 绿 / 紫 / 金四档数值，每次赋源至少出一条紫色。
  - 在铁砧上把赋源金源部件和**镀金 III** 装备合在一起，词条就加到装备上（最多三条，同类词条取较高值）；也能用合成把装备上的词条拆回部件里。
  - 镀金装备右键装水的炼药锅可以洗掉一级镀金（带词条的装备不能洗）。
- **勋章**（放在背包里就生效）
  - **幽灵勋章**：双击闪避键（默认 R）后 0.75 秒内免疫一切伤害，冷却 2.25 秒。
  - **刺客勋章**：按住冲刺键时双击方向键向该方向快速冲刺；双击 Tab 向上大跳，两者各自 3 秒冷却。
  - **月亮行者勋章**：反转重力、月球重力（重力 25%，最大下落速度为原版 40%）、空中按住潜行时双倍重力；持有时免疫摔落伤害。
  - **暗影勋章**拥有以上三枚勋章的全部能力。
- **符文**（放在背包里就生效）
  - **迅影符文**：基础移速 +15%，不受灵魂沙等方块减速，使用物品（举盾、拉弓、吃喝）时不减速。
  - **贪婪符文**：击杀掉落的经验翻倍；稀有掉落的概率 ×10，普通掉落再额外判定一次。
  - **收割符文**：目标生命低于 75% 时伤害 +12.5%，低于 50% 时 +25%。
  - **虹吸符文**：攻击命中回复最大生命的 5%（暴击 7.5%），每 0.45 秒最多一次。
  - **野兽符文**拥有四枚符文的全部能力；**永夜擢升之证**拥有暗影勋章和野兽符文的全部能力。
- **永不消耗的道具**
  - **永恒药水**（饮用 / 喷溅 / 滞留）：效果和合成用的药水相同，用完不消耗，冷却 10 秒（滞留型 20 秒）。
  - **终末之影珍珠**：不消耗的末影珍珠，冷却 10 秒。
  - **复苏图腾**：不消耗的不死图腾，放在背包任意位置都会触发，冷却 180 秒。
  - **虚空箭袋**：装入一种箭矢（或烟花火箭），当作无限的那种箭使用。
  - **自循环附魔之瓶**：右键花 10 点经验掷出一瓶，Ctrl + 右键把身上全部经验装瓶掷出（最多 32767 点），落地原样爆出。
  - **追溯指针**（原版物品）：右键传送回自己的重生点，找不到安全落点时回主世界出生点，冷却 10 秒。
- **钩爪弩炮**：拥有弩的全部功能。射出的箭命中后左键把自己以 32 格/秒拉过去；潜行 + 左键把命中的生物以 16 格/秒拉过来（带血条的 Boss 拉不动）。
- **测伤志愿者**：放下后是一只打不死的村民靶子，头顶显示过去 3 秒的 DPS 和单次伤害，潜行 + 右键拆回物品。
- **无线红石**
  - **金源无线连接装置**：在两个方块之间建立单向无线红石线路，可以跨维度；支持删除、反转方向、撤销 / 重做（Ctrl+Z / Ctrl+Y），操作距离 32 格。
  - **金源无线遥控开关**：绑定一个方块后远程使用：拉杆、按钮会被按一下，其它方块会输出 15 级红石信号。
  - 线路会随结构方块一起保存和放置。
- **指令**：`/orimodify <目标> module clear | install <词条> <数值> | remove <词条> | set <词条> <数值>`，直接修改手持物品的词条（需要管理员权限）。
  词条名：`atk` `critdmg` `dur` `tooleff` `pen` `arm` `armt`，数值单位是百分比。

1.21.1 版有客户端配置 `config/origolds_ar-client.toml`：`wireStyle` 选择无线线路样式，`NEO`（默认，发光线带）或 `CLASSIC`（原来的细线）；也可以在模组列表里点「配置」修改。26.x 版只有 NEO 样式。

### 支持的版本

| 分支 | Minecraft | 加载器 |
|---|---|---|
| [`1.21.1NeoForge`](../../tree/1.21.1NeoForge) | 1.21.1 | NeoForge 21.1.244+ |
| [`26.1.2NeoForge`](../../tree/26.1.2NeoForge) | 26.1.2 | NeoForge 26.1.2.103+ |
| [`26.3NeoForge`](../../tree/26.3NeoForge) | 26.3 | NeoForge 26.3.0.51-beta+ |

每个版本是仓库里的一个**分支**（`1.21.1NeoForge` 是默认分支，也是功能基准版本），分支根目录就是 Gradle 工程。
[CHANGELOG.md](CHANGELOG.md) 里有更新记录。

### 自己编译

需要 **Gradle 9.7+**（仓库里没有附带 Gradle Wrapper），以及 **JDK 21**（1.21.1）或 **JDK 25**（26.1.2 / 26.3）。切换到想要的版本分支，在根目录运行：

```
gradle build
```

jar 会生成在 `build/libs/` 里。

---

## English

Adds endgame mechanics and a whole pile of useful (and overpowered) gadgets to OriGolds. Enjoy the RNG, feel the thrill of a gadget race!

This is an add-on for [OriGolds](https://github.com/LW-Arqiu1shz/OriGolds) (金源工艺), which **must be installed**; install it on both the client and the server.

### Features

- **Infusion system**
  - Put a Broken Origold Part into the **Origold Assembly Station** and pick one of three offered traits to get an Infused Origold Part with three random traits.
  - 7 traits: Attack, Crit Damage, Durability, Tool Efficiency, Penetration, Armor, Armor Toughness, each rolled in one of four tiers (white / green / purple / gold); every infusion gets at least one purple.
  - Combine an Infused Origold Part with **Gilding III** gear on an anvil to add its traits to the gear (up to three; matching traits keep the higher value). A crafting recipe extracts the traits back into a part.
  - Right-click a water cauldron with gilded gear to wash off one gilding level (not possible once the gear has traits).
- **Medallions** (work anywhere in your inventory)
  - **Specter Medallion**: double-tap the dodge key (default R) to be immune to all damage for 0.75 s; 2.25 s cooldown.
  - **Assassin Medallion**: hold the dash key and double-tap a movement key to dash that way; double-tap Tab for a high leap. Each has its own 3 s cooldown.
  - **Moonwalker Medallion**: invert gravity, lunar gravity (25% gravity, terminal fall speed 40% of vanilla), and double gravity while holding sneak in the air; immune to fall damage while held.
  - The **Shadow Medallion** has all three medallions' abilities.
- **Runes** (work anywhere in your inventory)
  - **Swiftness Rune**: +15% base movement speed, no slowdown from soul sand and similar blocks, no slowdown while using items (shields, bows, eating).
  - **Greed Rune**: double experience from kills; rare drops are 10× as likely and common drops are rolled once more.
  - **Reaping Rune**: +12.5% damage to targets below 75% health, +25% below 50%.
  - **Siphon Rune**: hits heal 5% of your max health (7.5% on a crit), at most once every 0.45 s.
  - The **Beast Rune** has all four runes' abilities; the **Evernight Ascension Emblem** has everything the Shadow Medallion and Beast Rune have.
- **Never-consumed items**
  - **Eternal Potions** (drinkable / splash / lingering): same effect as the potion used to craft them, never used up; 10 s cooldown (20 s for lingering).
  - **Finis Pearl**: an Ender Pearl that is never used up; 10 s cooldown.
  - **Totem of Resurrection**: a Totem of Undying that is never used up and works from anywhere in your inventory; 180 s cooldown.
  - **Void Quiver**: holds one kind of arrow (or firework rocket) and acts as an endless supply of it.
  - **Self-Circulating Bottle**: right-click to bottle 10 XP and throw it; Ctrl + right-click bottles all your XP (up to 32767). It bursts back out where it lands.
  - **Recovery Compass** (vanilla item): right-click to teleport to your respawn point, or to the world spawn if no safe spot is found; 10 s cooldown.
- **Grapnel Ballista**: works like a crossbow. After the arrow hits, left-click to pull yourself there at 32 blocks/s; sneak + left-click pulls the mobs it hit toward you at 16 blocks/s (bosses with a health bar can't be pulled).
- **Volunteer Dummy**: an unkillable villager target that shows the last 3 seconds of DPS and the last hit above its head; sneak + right-click to pick it back up.
- **Wireless redstone**
  - **Origold Wireless Connector**: links two blocks with a one-way wireless redstone line, even across dimensions; lines can be deleted, reversed, undone and redone (Ctrl+Z / Ctrl+Y), within 32 blocks.
  - **Origold Remote Switch**: bind a block and use it remotely: levers and buttons get pressed, any other block outputs a level-15 redstone signal.
  - Lines are saved and placed together with structure blocks.
- **Command**: `/orimodify <targets> module clear | install <trait> <value> | remove <trait> | set <trait> <value>` edits the held item's traits (operators only).
  Trait names: `atk` `critdmg` `dur` `tooleff` `pen` `arm` `armt`; values are percentages.

The 1.21.1 version has a client config, `config/origolds_ar-client.toml`: `wireStyle` picks the wireless line style, `NEO` (default, glowing ribbons) or `CLASSIC` (the original thin lines). It can also be changed from the mod list's Config button. The 26.x versions only have the NEO style.

### Supported versions

| Branch | Minecraft | Loader |
|---|---|---|
| [`1.21.1NeoForge`](../../tree/1.21.1NeoForge) | 1.21.1 | NeoForge 21.1.244+ |
| [`26.1.2NeoForge`](../../tree/26.1.2NeoForge) | 26.1.2 | NeoForge 26.1.2.103+ |
| [`26.3NeoForge`](../../tree/26.3NeoForge) | 26.3 | NeoForge 26.3.0.51-beta+ |

Each version lives on its own **branch** (`1.21.1NeoForge` is the default branch and the reference version for features);
the branch root is the Gradle project. See [CHANGELOG.md](CHANGELOG.md) for the changelog.

### Building

Requires **Gradle 9.7+** (no Gradle Wrapper is included) and **JDK 21** (1.21.1) or **JDK 25** (26.1.2 / 26.3).
Check out the branch for the version you want and run, in the repository root:

```
gradle build
```

The jar is written to `build/libs/`.

---

## 贡献者 / Contributors

- **Arqiu-owo** — 设计、美术、文案、调试 / Design, art, writing and testing
- **Claude**（Anthropic）— 程序、项目管理 / Programming and project management
- **DeepSeek** — 程序、项目管理 / Programming and project management

## License / 许可证

- Code / 代码：[GNU LGPL v3.0](COPYING.LESSER)（with [GPL v3.0](COPYING)）
- Assets (images) / 素材（图片）：[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)

Details / 详情：[LICENSE.md](LICENSE.md)
