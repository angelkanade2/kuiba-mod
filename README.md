# 魁拔（kuiba）

NeoForge **1.21.1** 模组，M1「脉门」骨架。模组 id：`kuiba`。包名：`com.kuiba`。Java **21**。

## 用 IntelliJ IDEA 打开

1. 安装 JDK 21，并在 IDEA 里设为项目 SDK。
2. `File` → `Open`，选中本目录（含 `build.gradle`、`gradlew` 的那一层），不要选上一级。
3. 以 Gradle 项目导入。第一次会下载 Minecraft、NeoForge 和 Parchment，需要网络，时间会比较长。
4. 导入完成后，Gradle 工具窗口里会出现 `runClient`、`runServer`。
5. 游戏语言选「简体中文」，物品和界面才会显示中文。

Windows 上如果脚本没有执行权限，用 `gradlew.bat`，不要用 `./gradlew`。

## 运行客户端

```bash
./gradlew runClient
```

Windows：

```bat
gradlew.bat runClient
```

服务端（无界面）：

```bash
./gradlew runServer
```

构建 jar：

```bash
./gradlew build
```

产物在 `build/libs/kuiba-0.1.2.jar`。

按 `R` 打开脉术修炼界面。按 `G` 开脉门：脉力补满，并播放开脉音效（门阶至少 1，消耗 2 点饥饿，1 秒冷却）。界面左下角的「开脉」做同一件事。脉力每秒掉 `上限 × 0.04`；织脉两件套掉得更慢（×0.90）。死亡后脉力归零，冲脉成功会免费补满一次。

## 键位与物品

| 操作 | 方式 |
| --- | --- |
| 脉术修炼界面 | `R` |
| 开脉门 | `G`（或界面里的「开脉」） |
| 施放槽位 1 / 2 / 3 | `V` / `B` / `N` |
| 开始冲脉 | 右键「冲脉丹」，或界面里的「冲脉」 |
| 取消冲脉 | 潜行右键冲脉丹，或界面里的「取消冲脉」 |
| 连招 | 见下方「技能圆盘」 |
| 技能圆盘 | 按住 `Alt`，鼠标滑到连招上松开 = 选中 |
| 施放选中的连招 | `Z` |
| 连招 2（固定槽位） | `X` |
| 连招编辑 / 起名 / 放入圆盘 | `J` |

创造模式物品栏有「魁拔」分页：脉晶、脉碎片、脉核、冲脉丹。材料只有这三样，没有 `condensed_*`。门阶 3 和 4 的仪式暂时用 `minecraft:leather` 加脉晶代替绒毛。

### 材料来源

- **脉晶**：新玩家第一次进服会拿到 **6 个**（正好够开门阶 1），由 `startingCrystals` 配置。此外**玩家击杀僵尸**有概率额外掉 1 个，概率是 `zombieCrystalChance`（默认 `0.15`）；只算玩家击杀，日出烧死/淹死的不掉。僵尸变种（尸壳、溺尸、僵尸村民）都算，僵尸猪灵不算。
- **脉碎片 / 脉核**：暂时仍然没有来源，只能创造模式拿或 `/give`，门阶 5 之后的冲脉材料因此还是断的。
- 以上两个数值都在 `config/kuiba-server.toml`，可以热改。

### 技能圆盘

连招可以存 **8 条**，每条带名字（名字空着就显示第一个技能的名字）。按 `J` 开编辑器：左栏点选要编辑的那条，右栏起名、选「倍率 / 序列」、排最多 **5 步**、给每一步单独设倍率、调步间隔，点「放入圆盘」把它挂到圆盘上。改动都是本地的，点「保存」才发给服务端；服务端会重查解锁状态、每一步的倍率、并把总消耗和脉力上限对一遍。保存和放入圆盘都**没有门阶限制**，唯一的门槛是**总消耗不能超过当前脉力上限**。

按住 `Alt` 打开圆盘：每格显示这条连招**第一个技能的图标 + 名字**，右上角标步数，当前选中的那格有金框。鼠标滑到某一格上松开 `Alt` 就选中它；滑在中心空白处松开、或按 `Esc`，都保留原来的选择。之后按 `Z` 施放当前选中的连招，`X` 仍然固定施放第 2 条。

圆盘做成 `Screen` 是有原因的：原版 `setScreen` 会调用 `KeyMapping.releaseAll()` 并释放鼠标，所以"松手"以 `keyReleased` 的形式收回来最可靠，鼠标也能自由移动来划格。副作用是打开期间不能边走边按技能。

冲脉材料写在 `data/kuiba/mai_gate/tiers.json`。创造模式不消耗材料；生存模式成功时才扣除。移动超过 2 格、受伤或断线会中断：不扣材料、不加门阶、不加反噬（裁定 A）。脉力条画在饥饿值上方，技能图标带冷却遮罩。R 界面可点击或拖拽装备，并列出下一门阶的材料。

## 本阶段已经接上的部分

1. **Attachment + 同步 + 配置**
   - 玩家数据 `kuiba:mai`（`AttachmentType`，`copyOnDeath`）。
   - 字段：`gate`、`current`、`equipped[4]`、`unlocked`、`backlashStacks`、`backlashStickyUntil`、`lastCombatTick`、`regenPauseUntil`、`cooldowns`、可选 `ritual`、`wenyaoSlot`、`combos`。`comboRuntime` 只活在内存里。
   - **不保存** `max`，上限由门阶表现算。
   - 死亡只留下门阶、已解锁、已装备；复活后 `current = 0.30 × max`，清空反噬、冷却和仪式。`keepGateOnDeath=false` 时门阶也重置。
   - 进服、复活、换维度时用 `SyncMaiPayload` 同步。回血大约每 10 tick 同步一次。
   - `getMax(player)` = 门阶表 `max_points` + `WenyaoApi.bonusMax`（现在返回 0）。
   - `tryConsume` 先乘 `getCostMultiplier(player, skillOrArt)`，默认 1，不够就失败，不透支。
   - 脱战回血再乘 `getOocRegenMultiplier`，默认 1。以后护甲两件套走这里，计划是 ×1.10。战斗回血不乘。
   - `wenyaoSlot` 是一个空的 `ItemStack`，会保存。`keepWenyaoOnDeath=true` 时死亡复制保留它。界面装备以后再做。
   - 连招预设 `combos` 共 8 条、每条最多 5 步，会保存。`comboRuntime` 不写进 NBT，死亡和断线都清掉，因此不会 `copyOnDeath`。
   - 配置：`config/kuiba-server.toml`（`ritualMode=simple`、`keepGateOnDeath=true`、`keepWenyaoOnDeath=true`、战斗/脱战回血比例）。
   - 门阶变化时按 `tiers.json` 的 `modifiers` 挂攻击伤害和移动速度（`AttributeModifier`）。

2. **门阶 / 技能重载 + MaiApi**
   - `data/kuiba/mai_gate/tiers.json`：门阶 0–12。
   - `data/kuiba/mai_skill/*.json`：九个 P0 技能。未知 `cast` 只打警告并跳过，不让游戏崩。
   - 客户端和服务端都挂了 `SimpleJsonResourceReloadListener`。
   - `MaiApi`：`getGate`、`getPoints`、`tryConsume`（创造不耗脉力，不够则失败，没有透支）、`meetsGate`、`addBacklash`（空实现）。
   - 事件：`MaiGateChangedEvent`、可取消的 `MaiCastEvent`。
   - 进度触发器 `kuiba:gate_reached`。

3. **脉晶 + 冲脉丹仪式**
   - 冲脉丹走同一套 `MaiRitual` 状态机。祭坛模式只在配置里留了 `ALTAR`，P0 仍走丹药。
   - 网络包：`StartRitualPayload`（开始/取消，可带方块坐标）、`RitualProgressPayload`（进度或取消）。
   - 成功：扣除 JSON 材料，门阶 +1，重算属性，`current = 新上限 × 0.5`，同步，并触发 `gate_reached`。
   - 回血：战斗 `0.02×max/秒`，脱战（100 tick 内没有造成或受到伤害）`0.08×max/秒`，受伤后停 40 tick。

倍率分 **×1 / ×2 / ×3 / ×5 / ×10** 五档，**倍率本身就是消耗倍数**（×10 就是十倍基础消耗），效果倍率分别是 1、1.75、2.75、4、5.5 —— 高倍率花得多、涨得慢。倍率**不再需要门阶**，能不能用只看脉力够不够；连招保存也只看总消耗是否超过脉力上限。槽位施放和连招每一步都走 `MaiCast.execute`。序列中途失败不退还已经花掉的脉力。解释器先处理 `dash` / `buff`（脉闪、脉护），再处理其余七个技能；`aoe_zone` 的写法保留，留给以后的数据包技能。

脉闪朝你**按住的方向键**冲：`W` 前、`S` 后、`A` 左、`D` 右，`W+A` 这类斜向会归一化成 45° 方向；按方向键时以水平为主，但**保留一半的视线俯仰**（抬头会带一点上抛，低头不会下扎）；**一个方向键都不按就完全按视线方向冲**。方向是**每次施放实时判断**的：客户端只在按键组合变化时推一个 `MaiInputPayload`（W/S → `forward`，A/D → `strafe`），服务端把它存在玩家数据里，连招里隔几秒后的后几步也按当时按着什么来定方向（服务端拿不到普通移动输入，只有骑乘时才有，所以必须由客户端推）。施放脉闪后还有 **60 tick 的坠落免伤**，只在窗口内第一次落地生效。

纹耀用 `/kuiba wenyao equip` 把主手挂件放进槽，`/kuiba wenyao clear` 取下。门阶够了才会加脉力上限和降消耗。脉裂刃、凝脉枪主手右键施放武器技，没有第二套按键。织脉护甲两件套让脱战回血 ×1.10，四件套命中回复 0.3 脉力。

4. **施放 + HUD + 装备界面**
   - `RegisterGuiLayersEvent` 注册 `MaiHud`。
   - `RegisterKeyMappingsEvent` 注册 R / G / V / B / N / Z / X / J / Alt。
   - 连招：`MaiCombo` 管 8 条带名字的预设和圆盘选取，`SaveComboPayload` 单条保存，`SelectComboPayload` 提交圆盘选择，`SyncMaiPayload` 带上全部预设和当前选中项。
   - 脉门界面可点选已解锁技能，再点槽位装备（`EquipSkillPayload`）。施放走 `CastSkillPayload`。
   - 施放会扣脉力、进冷却，并套一层很短的原版效果（冲刺、近战、雪球、治疗、状态效果）。这是能编译、能看出来的占位，不是完整技能。
   - 门阶 7–12 的数据已加载，界面上这几格是灰的。

## 故意没做的部分

反噬和透支（步骤 5 / P0.5）只有字段、同步和 `MaiApi.addBacklash` 的日志占位，不会叠层，也不会在中断时追加反噬。

## 同步到 Windows

仓库里的 `artifacts/kuiba-src.zip` 是源码包，已排除 `.gradle`、`build`、`.git`。解压后用 IDEA 打开该目录，再执行上面的 `gradlew.bat runClient`。不要把本机的 `.gradle` 或 `build` 和压缩包混在一起拷。
