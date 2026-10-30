# 通用生物施法与魔符能力标签规划

日期：2026-10-31。状态：首批已实现（Core 0.1.44 / Ars Instinct 0.1.0），后续资源生命周期提取仍在规划中。

Core 基线：0.1.43 / `93cf3e9340662c335de70249614e5a9d56fe7b3b`。
首个迁移来源：相邻 `Beneath-The-Dying-Sun` 项目已有的生物施法实现。

独立通用 AI 附属定名：**Ars Instinct**，modid **`ars_instinct`**，计划工程目录 `MinecraftDev/Ars-Instinct`。本地工程已建立并纳入 Git；尚未创建公共项目或发布。

首批实现：Core 标签注册/重载，Instinct 通用控制器与 Ground Goal，原生工具后端，Iron 首批能力提示，落日之下选槽与目标判断接入。整合包继续保留 EntityMana、固定工具执行、非玩家 Iron 计时和物种数值；并未宣称完成下表全部迁移。

## 1. 已确定的边界

- 优先完善现有通用能力，暂停新增 Apprentice's Codex / Spell Engine 联动。
- 附属为单个魔符提供大致能力标签；允许多个标签、允许不声明。
- 术式用法继续使用简单规则：起始形态 + 第一个关键能力，不引入完整术式推理器。
- Core 提供标签、资源和执行接口；Ars Instinct 提供简单编排判断、多槽选择、攒蓝、瞄准、友军治疗、近战/施法切换的通用 Goal 或控制器。
- Ars Instinct 只直接依赖 Ars Nouveau + Ars Conflux，不硬依赖 Iron、Goety 或落日之下。各法术附属通过标签和公共接口参与，纯 Ars 编排也可使用。
- Core 不包含 AI 决策或自动安装 Goal。Ars Instinct 默认不接管所有生物，由配置、数据或其他模组显式接入；接入策略、生物配装/数值、警戒仇恨和特殊行为留在整合包或冒险内容模组。
- 复用落日之下已有实现，尤其继续使用原生 Ars Mana 附件，不新增第二套平行魔力存储。
- 通用能力不依赖 Iron / Goety。附属自己的属性、费用和原生限制仍由对应附属解释。
- 同步维护 1.21.1 NeoForge / 1.20.1 Forge 的公共语义；平台差异通过平台层处理，不使用假玩家伪装非玩家施法。

## 2. 当前已有能力与迁移位置

| 现有位置 | 已有职责 | 规划去向 |
|---|---|---|
| beneathsun/compat/ars/EntityMana | 非玩家 Ars 附件访问、按需激活、上限计算、每秒恢复 | 通用部分进入 Core；种族数值留整合包 |
| beneathsun/magic/ManaPools | 查询、扣费、恢复、真实存量转移 | 抽取或适配到 Core 统一资源入口，保持原生底层 |
| beneathsun/compat/ars/FixedSpellCaster | 真实生物执行、固定工具的原生 Ars resolver | 抽出执行基础；封存/编辑限制留物品玩法层 |
| beneathsun/compat/ars/SealedSpellTools | 工具识别、使用许可、预检、非玩家 Iron 冷却 | 通用执行转 Core；Iron 规则转 Iron provider；物种等级规则留整合包 |
| beneathsun/compat/ars/ProgramTactics | 依据形态/第一个效果选敌我、需要治疗等 | 通用判断迁入 Ars Instinct，消费 Core 标签；特殊规则留整合包 |
| beneathsun/compat/ars/ToolCombatCasting | 多槽候选、预算、攒蓝、共享冷却避让 | 通用选择迁入 Ars Instinct，预算/计时查询复用 Core；物种权重由调用方提供 |
| beneathsun/world/SpellMeleeGoal | 追敌、近战、抛物线瞄准、决策频率 | 通用 Goal 迁入 Ars Instinct，安装范围与特化由调用方决定 |

当前 EntityMana 里巨型水母 500、末影人 240 等硬编码、特定 Iron 属性、工具配装、首槽权重和连续重复惩罚都不原样搬进 Core。

Core 的 CastContext / ExecutionSession / ActiveCasts 已接收 LivingEntity。应扩展、接线现有入口，避免新增平行调度器。

## 3. 第一阶段：只提供魔符能力标签

### 3.1 数据模型

能力标签是 `ResourceLocation` 集合，挂在 Ars 的魔符注册 ID 上。

- 接受效果、形态、增强等 AbstractSpellPart 的身份；实际标注按需要提供。
- 不使用固定 Java enum 封死扩展空间。
- 不等同于物品标签：AI 需要描述的是编排节点，不能仅依赖手持魔符物品。
- 与 Ars School、Conflux 流派、伤害类型互不代替。标签不改变它们。
- 查询不存在的魔符或未声明的标签返回不可变空集合，表示缺少提示。

拟提供 `GlyphAbilities` 接口（名称/签名为实施草案）：

```java
void declare(ResourceLocation glyph, Set<ResourceLocation> tags);
Set<ResourceLocation> get(ResourceLocation glyph);
Set<ResourceLocation> get(AbstractSpellPart glyph);
boolean has(AbstractSpellPart glyph, ResourceLocation tag);
```

`declare` 为附属在初始化时提供默认值；同一身份的冲突声明报清楚来源，不按不可控加载顺序静默覆盖。数据包用于最终修改。

### 3.2 首批通用词汇

| 标签 | 含义 |
|---|---|
| ars_conflux:attack | 能力主要或部分用于伤害敌人 |
| ars_conflux:healing | 恢复生命 |
| ars_conflux:defense | 防护、抵挡或提高承伤能力 |
| ars_conflux:control | 限制或改变目标行动 |
| ars_conflux:summoning | 产生召唤物 |
| ars_conflux:mobility | 移动、冲锋、传送等机动能力 |
| ars_conflux:utility | 其他工具用途 |
| ars_conflux:area | 有作用范围，例如自选型群体治疗 |
| ars_conflux:channeling | 运行期间持续执行的能力，不是单纯吟唱时间较长 |
| ars_conflux:receiver_self | 可将能力施加给自身 |
| ars_conflux:receiver_ally | 可将能力交给友军接受/执行 |
| ars_conflux:receiver_enemy | 面向敌方接收目标 |
| ars_conflux:receiver_position | 可以在位置上执行 |
| ars_conflux:player_only | 现有实现需要玩家身份，供 AI 提前排除 |

这些是提示词汇，不是必须全部填写的完整规范。第三方可以增加自己的命名空间标签。

用途与接收对象分别表达。例如火焰吐息可以同时是 attack、channeling、receiver_self、receiver_ally、receiver_position：给友军执行并不使它失去攻击用途。最终标签要核对具体适配实现后声明。

### 3.3 数据包格式

采用按魔符的一份简单描述，与现有 GlyphSchools 的身份寻址方式一致。

示例路径：

```text
data/irons_nouveau/conflux/glyph_abilities/glyph_fire_breath.json
```

已实现的数据格式：

```json
{
  "tags": [
    "ars_conflux:attack",
    "ars_conflux:channeling",
    "ars_conflux:receiver_self",
    "ars_conflux:receiver_ally",
    "ars_conflux:receiver_position"
  ]
}
```

路径对应 `irons_nouveau:glyph_fire_breath`；不把它转换为原生 Iron 法术 ID，也不丢弃命名空间。

覆盖规则保持简单：

1. 代码声明作为默认值。
2. 存在数据文件时，以其 tags 完整替换该魔符的默认集合；同路径沿用资源包优先级。
3. `{"tags": []}` 明确清空。
4. 移除覆盖文件后重新加载，恢复代码默认值。
5. 未安装附属的魔符 ID 可以保留为休眠声明，查询实际编排时自然不会遇到，不因此报启动错误。
6. ID/JSON 错误提示明确文件；坏条目不按有效空覆盖处理。其余有效条目正常加载。
7. 重载先构建不可变快照再替换；服务器停止时清理，不能跨存档遗留覆盖。

第一版不加入嵌套标签、复杂选择器、每个法术的结构化 AI 配置或动态数值提供者。标签查询首先供服务端使用；本阶段不增加客户端展示或同步协议。

## 4. 标签在 Ars Instinct 与落日之下的消费方式

保留现有简单模型：

1. 起始形态决定触摸、弹射物、射线或自身的基本操作。
2. 先识别该形态绑定的弹射物替换增强；存在时，它属于主能力，而非应跳过的普通增强。
3. 没有替换时，取第一个关键效果。普通增强不主导接收目标。
4. 读取该节点的标签决定攻击、治疗、自保或给友军执行。
5. 不把所有后续标签并集成一个统一目标，不展开复杂嵌套或模拟完整运行结果。

例如“弹射物→火球替换→治疗”不能因为跳过全部增强而被判断成普通治疗弹；“自身→吐息”是以自身为执行者的攻击。

标签只描述能力，不能替代真正的执行许可、实体检查、资源检查或友军验证。数据包把玩家专属魔符误标成 receiver_ally，也不能使它绕过原实现的限制。

未描述或当前 AI 不认识的组合保持未知；不猜成“伤害为零”“费用为零”或“默认攻击敌人”。玩家手动施法不因缺标签被禁用。

第一阶段不把射程、弹速、重力、状态剩余量、治疗阈值做进标签系统。落日之下已有对应判断可暂保留；需要扩展时使用独立接口，不把标签变成带任意参数的脚本语言。

## 5. 第二阶段：迁移生物 Ars 魔力基础

- 使用现有 Ars Mana 附件作为实际存储，保持与玩家单位一致。
- 按需激活非玩家池；已有附件或已有存量时保留，读档/重复查询不重新补满。
- 上限与恢复调用当前版本原生事件/属性入口；平台差异留平台层。
- 默认值来自 Ars 或外部显式策略。整合包的物种 240/500 等通过整合包策略提供。
- 恢复只更新已激活且存活的服务器生物，沿用错峰低频计算；不向所有生物每 tick 安装复杂状态。
- 扣费接 ResourceAccount / ResourceTransactions，预估与实扣共用池键和报价。
- 转移使用真实存量，创造模式也不能凭转移凭空复制魔力。
- 落日之下停止注册自己的重复提供者与恢复 tick，转成 Core 薄封装；迁移保留原附件和历史激活状态。
- 读取原生池时尊重其他模组已有提供者，避免两个生命周期同时恢复。

## 6. 第三阶段：统一非玩家主动施法入口

围绕现有 ActiveCasts / CastContext 封装，而非新增另一套吟唱、会话和派发。

入口接收：实际 LivingEntity、手持工具/手、实际编排，以及必要的目标或方向；返回立即释放、正在吟唱、资源不足、冷却中、工具改变或不支持等明确状态。

- 读取 Ars 原生 SpellCaster/工具编排，不要求是落日之下专用骨棒或权杖。
- 接入工具的使用授权，不给普通生物制造玩家学习记录；封存工具与可编辑法术书各自保留许可边界。
- 保留 caster、executor、damageOwner、account；选中友军不自动更换付款者。
- 延迟释放前重新验证生物、世界、工具与必要目标；死亡、换维度、卸载和中断时清理。
- 预估调用现有 SpellResourceQuotes 和真正账户；未知费用显式保留，避免继续在整合包中只累加 Iron 原始 Mana。
- 持续能力按实际触发结算，不把整段施法一次性预付的要求强加给所有能力。

Iron 的非玩家冷却接入自己的 ActiveCastProvider，使用公共持久冷却基础保存原生键，保留等级、原生属性缩减和重施规则。落日之下对应的额外记账随后退出，避免两个冷却同时计时。

Goety 保留灵魂能量/Ars Mana 的现有配置语义。普通生物没有玩家灵魂账户时，必须由使用方明确提供账户或选择已有可用资源模式；不悄悄免单，也不未经设置自动转换资源。玩家界面等专有法术继续明确拒绝非玩家。

## 7. 实施顺序与验收

### 批次 A：标签闭环

交付 Core 查询 + 数据覆盖，并由 Iron 给落日之下当前识别的能力提供默认标签；Ars Instinct 抽取简单 AI 判断，落日之下作为首个调用方，替换 ProgramTactics 中相应的用途/敌我硬编码。第一批先建立这条闭环，不要求同时完成全部魔力和冷却迁移。

验收：

- 自身吐息、射线治疗、射线授予行动、触摸攻击维持原用途。
- 弹射物替换增强不会被当作普通增强跳过。
- 数据覆盖/清空/移除恢复、未知标签与缺席附属可用。
- Core 不加载 Iron 类；不改变实际费用、冷却、伤害或 AI 安装范围。
- 公共规则两平台一致。

### 批次 B：原生魔力迁移

复用已有 EntityManaGameTests 的关键语义：保存空池、恢复、容量缩小、创造扣费/真实转移；补重复提供者/恢复退出与落日之下调用检查。迁移原有原生附件，不重置生物存量。

### 批次 C：执行与附属计时

先用普通生物执行同一本含 Ars + Iron 编排的工具，验证真实身份、一次吟唱、一次冷却、持续扣费、中断；随后处理 Goety 的非玩家账户扩展与明确拒绝边界。

客户端表现只在涉及新同步、动画或持续渲染时做对应观察；服务端通过不宣称视觉已验证。不为每个文档或标签反复启动完整客户端，也不跑全 Boss 矩阵。

## 8. 本次规划不包含

- 新增生物、法术或其他模组联动。
- 给全体生物自动附加施法 AI 或改变自然生成配装。
- 完整编排语义分析、复杂效用评分或自动配法术。
- 将所有模组原生资源合并为一个无条件通用池。
- 立刻迁移所有附属或承诺所有玩家专有法术支持生物。
- 构建、部署或发布新的模组版本。

文档落地后，推荐从批次 A 开始编码。后续需要的通用功能沿这份顺序推进，避免标签这一小步扩大为一次全部 AI 重写。
