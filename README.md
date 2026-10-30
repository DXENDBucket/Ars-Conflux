# Ars Conflux

0.1.40 adds shared, synchronized cooldown and cast-time reduction attributes. Provider-native timing is calculated first; Conflux applies the shared reduction once. Defaults are neutral. See [timing attributes](docs/timing-attributes.md).

0.1.38 fixes a Forge 1.20.1 startup regression in 0.1.37: restored Gradle compilation caches now include the generated Mixin refmap. Forge packaging and release preparation reject missing refmaps. The NeoForge build is version-aligned; gameplay and translations are unchanged.

0.1.37 adds Simplified Chinese language supplements for Ars Controle and Ars Additions, including missing spell descriptions, UI messages and guide pages. Forge receives the translations missing from its older addon releases. No addon becomes mandatory and casting behavior is unchanged. See [localization coverage and licensing](docs/optional-addon-localizations.md).

0.1.36 connects Zero Push/Anchor/Remove Gravity to shared projectile controls, carries Beam/Voxel impact directions into delayed continuations, and recognizes Zero's optional high-tier augments. See [motion and direction API](docs/projectile-controls.md).

0.1.35 extends Zero's repeat-billing correction to Geometrize and connects its reflective Arcane Shield to managed Conflux flights. The shared [reflection API](docs/projectile-reflection.md) keeps the original resource account while transferring damage credit; unsupported fixed trajectories are absorbed.

0.1.34 fixes the Ars Zero staff/circlet glyph grid: every category starts a row, page counts include category icons, and addon categories and provider ordering are retained.

0.1.33 adds optional Ars Zero 2.0.2 integration on NeoForge 1.21.1: shared preparation and cooldowns for a staff/circlet use, beam billing without duplicate charges, and runtime native fixes with preceding-execution temporal targets. Zero remains optional. See [scope, configuration and validation limits](docs/ars-zero-compatibility.md).


0.1.32 adds optional native-augment extensions for AAP / NEG, weighted Controle delays, and the opt-in `AugmentSupport.withReductions` API. The new compatibility behavior defaults on and can be disabled with `compatibility.enable_augment_extensions = false` in `ars_conflux-startup.toml` on both sides before restarting. Details: [augment extensions](docs/augment-extensions.md).

为 Ars Nouveau 魔符附属提供共享的执行、触发计费和弹射物兼容 API。首个接入者为 Iron & Nouveau；未来其他法术附属可以独立依赖 Conflux，不必安装 Iron，也不必为彼此重复实现 Ars 载体兼容。

Mod ID：`ars_conflux`。作者 EndXiom，代码使用 [MIT 许可证](LICENSE)，附属译文单独保留 LGPL-3.0 声明。当前版本 **0.1.40**。

本地更新工具：双击 `Publish-CurseForge.cmd` 可构建并上传 Core 与附属的双版本发行包，设置方法见 [CurseForge 发布说明](docs/curseforge-publishing.md)。

| 游戏版本 | 加载器 | Java | 发行文件 |
| --- | --- | --- | --- |
| 1.21.1 | NeoForge 21.1.250 | 21 | `ars-conflux-0.1.40-mc1.21.1-neoforge.jar` |
| 1.20.1 | Forge 47.4.0 | 17 | `ars-conflux-0.1.40-mc1.20.1-forge.jar` |

需要 Ars Nouveau 及其前置。Iron、Goety、Not Enough Glyphs、Ars Elemental、Ars Controle、Ars Additions 都不是 Core 的必需依赖。

0.1.31 新增 Adam's Ars Plus 的可选兼容：II／III 级增强按 2／4 份原版增强参与提供者的数值与校验；领域与交换目标延续解析器来源和入口策略，并修正领域子编排的读取位置。Iron 0.17.28、Goety 0.1.1 接入共用接口，不引入附属间依赖。详见 [兼容范围、费用与验证](docs/adams-ars-plus.md)。

## 魔符图标

0.1.30 将二维外框与贴图图标改用不受物品方向光影响的 GUI 绘制，修正部分三维来源魔符外框偏暗。原生三维物品保留原有光照、变换和透明度。两平台均适用。

0.1.26 将「周围友军」接入相同外框，并修正候选魔符预览：前方载体上的新错误也会让导致该错误的候选魔符变暗。原施法校验、错误文本和增强说明来源保留。


0.1.25 为已通过 `GlyphPresentation` 或 `GlyphVisuals` 接入的附属提供 Ars 原版底板和分类外框；中心默认缩至 75%，普通 Ars 魔符不变。支持三维物品、无效编排淡化和资源包重载。Iron & Nouveau 0.17.21 另提供原生法术图标。

`config/ars_conflux-client.toml` 可独立控制外框、中心来源、缩放、统一颜色或分类颜色。只影响 GUI；见 [客户端配置与图标 API](docs/glyph-visuals.md)。

## 可选通用魔符

Conflux 0.1.16 提供三级形态「周围友军」和通用友军判定 API。物品、魔符注册和抄写配方默认关闭，必须在客户端与服务器同时开启并重启：

```toml
# config/ars_conflux-startup.toml
[optional_content]
enable_nearby_allies = true
# 仅 NeoForge：让 Ars Elemental 注册其隐藏的卡利亚圆阵。
enable_elemental_phalanx = true
```

两个开关的发行默认值都是 `false`。没有 Elemental 不会注册卡利亚圆阵；Forge 当前版本没有此上游魔符。

「周围友军」默认半径 8 格，每个扩散增加 3 格（上限 32），排除自身、死亡和旁观实体；每个目标独立执行剩余编排。基础形态费用为 30 Ars Mana，后续效果仍按 Ars／各提供者的计费规则执行。默认 Ars 原生效果仍是整次施法预付；启用触发计费后则逐目标结算。此形态不改变友军伤害规则，也不转移施法者、伤害归属或资源账户。

抄写台材料：召唤精华、操纵精华、魔源宝石各一，经验成本 160；施法遵循 Ars 原版三级魔符限制。获取配方仅在开关开启时加载，可供 JEI 显示。详细接口和语义见 [友军目标 API](docs/allied-targeting.md)。

## Ars ’n Spells 载体联动

0.1.30 为 ANS 3.4.0 的导出卷轴、铭刻物品和原生 Iron 法术轮接入统一吟唱与冷却边界；保留 ANS 制作、选点、资源倍率及原生代理槽冷却。没有 ANS 时不启用这些挂钩。详见 [适配范围与验证](docs/ars-n-spells.md)。

## 当前内容

- 显式接入的原生法术伤害 API：统一 Ars 装备加成、Pre/Post、上下文附件和同步伤害作用域；普通原生攻击不变。见 [原生伤害 API](docs/native-spell-damage.md)。

- 公共执行会话：启动、逐 tick 更新、权限复查、到期、维度卸载和服务器停止清理。
- 通用资源账户、同池预留、触发成功后扣款、失败释放以及可负担数量计算。
- 提供者首次触发费用的只读预估，按实际资源池合并判断余额，未知报价明确保留。
- 区分施法者、实际执行实体、伤害所有者与支付账户的上下文；Iron 已接入，克隆、接续和持续执行保留归属。
- 可注册的弹射物载体与载荷接口；复用 Ars 轨迹、运动及碰撞，保留附属的后续编排。
- 共享组合验证入口和散射预算，散射角度仍由 Ars 原有逻辑决定。
- Ars 原版弹射物、环绕，以及可选的弹跳、追踪、溅射、调态节点、回响轨迹支持。
- Ars Elemental 的弧线／追踪再次发射预算，以及卡利亚圆阵（1.21.1）的轨迹兼容；0.1.14 默认通过独立启动配置启用上游隐藏的圆阵，并加入抄写台配方。
- Ars Controle 传送棱镜的替换弹射物跨维度迁移；Ars Additions 反击在受伤回调中选中本次攻击者。1.21.1 另修正 Controle 二元随机过滤的增强位置与概率覆盖，1.20.1 不移植缺失功能。详见 [兼容边界](docs/controle-additions.md)。
- 可选的原版 Ars 效果触发计费，包含每层嵌套环绕的创建费用；默认关闭。
- 公开的 `ArsSpellAccess`：读取法术部件快照，访问弹射物和法术箭解析器，供附属共用源码。
- 通用解析器来源附件，保留派生、碰撞包装和报价所需的入口语义。
- Hex-Ars Linker 的常规、指定位置及指定实体施法、回调深度与计费共用 Core 适配；支持可选原生 Ars 触发计费，法术附属不必分别适配 Hex。详见 [适配范围与独立验证](docs/hex-ars-linker.md)。

法术的具体伤害、状态、粒子、原生实体钩子、等级、学习方式、吟唱与冷却仍由法术附属负责。Core 不自动把任意原生法术变成魔符；Goety & Nouveau 已接入灵魂箭及多种效果、召唤魔符，两种法术附属可同时使用一份 Core。

## 资源边界

触发计费是公共能力，费用定义和资源来源由调用方提供，因此既可用于 Iron，也可用于原版 Ars 或其他附属。当前 Iron 已使用这套事务接口。

**默认保持原版 Ars 的起手计费。** 在世界的 `serverconfig/ars_conflux-server.toml` 中可独立启用：

```toml
[ars_trigger_billing]
enabled = false
```

改为 `true` 后，原版效果及其书写增强的费用移到各次效果执行。起手形态仍收费，弹射物未命中不会收取未执行的效果费用；环绕每次创建内层球体都会再次检查费用，余额不足则停止当前分支。创造模式免费。Iron 等附属沿用各自的计费实现，魔源装置保留原生规则。

这里的“触发”是 Ars 的效果执行入口，正常返回即视为一次付费尝试；原版没有通用成功返回值，所以对不适用目标执行的效果也可能付费。单个效果内部的多目标伤害或自动持续状态不另按每个目标、每 tick 收费；持续载体每次重新执行编排则逐次收费。详见 [配置与计费语义](docs/trigger-billing.md)。

详见 [API 与责任边界](docs/api.md)、[原生法术接入核对](docs/spell-adapter-checklist.md)、[构建说明](docs/building.md) 和 [版本记录](CHANGELOG.md)。

## 版本策略

两个平台共用根目录 `VERSION`。0.x 阶段 API 仍在演进：破坏兼容的变更提高次版本号，兼容修复提高补丁号。Iron & Nouveau 0.17.0 要求 Conflux `[0.1.0,0.2)`。

版本由独立 Git 仓库管理，发布节点使用 `v0.1.8` 这样的标签；不会将上游依赖、运行世界或本地构建缓存提交到仓库。Iron & Nouveau 0.17.7 要求 Conflux `[0.1.8,0.2)`；Goety & Nouveau 0.1.0-dev.5 的最低要求仍为 `[0.1.8,0.2)`，可以共用 Core 0.1.8。

0.1.7 提供不触发客户端假玩家创建的上下文读取 API，并将散射预算和原生效果计费限定在服务端。排查范围和验证限制见 [端侧异常排查](docs/side-boundaries.md)。

0.1.8 新增共同的魔符展示排序入口。Iron 和 Goety 只登记自己的分组与内部排序键，不各自修改 Ars 页面；同类中普通 Ars／附属在前，Goety、Iron 各自集中在后。魔符类型和解锁等级不因展示排序改变。

主动吟唱与冷却：[实现与边界](docs/active-casting.md)

跨附属维护：[2026-10-13 横向源码审计](docs/all-addon-audit-2026-10-13.md)记录固定版本下的资源、执行授权、冷却反馈和生命周期差异，区分待修问题与既有设计；审计建议不代表已实施的行为变更。

NEG 的织册、派生分支、再次发射预算及双版本适配详见 [兼容范围与定向验证](docs/not-enough-glyphs.md)。

Elemental 的再次发射与卡利亚方阵详见 [兼容范围与定向验证](docs/ars-elemental.md)。

## Forge 内嵌库兼容修复（0.1.19）

MixinExtras 的 Jar-in-Jar 版本范围改为 `[0.4.1,)`，继续自带 0.4.1 作为最低可用实现，但允许整合包选用 0.5.x 或更新版本。原来的 `<0.5` 上限与其他模组要求 0.5.5 的范围无交集，导致 Forge 在模组预加载阶段退出。本次不改变魔符行为或配置。

0.1.21 新增中立的执行锚点与实际实体占用，参见 [执行锚点](docs/execution-anchors.md)。实体／位置来源不改变付款和伤害归属，动作和持续引导可按不同通道声明。

## 跨附属交互修复（0.1.22）

统一冷却拒绝信息并显示最长剩余冷却；提供 `MovementLimits` 合并混合吟唱的移动限制。Hex-Ars Linker 的新主动操作接入共同准备和冷却边界，被动命中回调保持原语义。世界 `serverconfig/ars_conflux-server.toml` 有独立开关：

```toml
[hex_linker]
chanting = true
cooldowns = true
```

开关只控制 Hex 入口；各提供者的配置仍生效，资源消耗不变。详细修复与验证记录见 [交接](docs/handoff-cross-addon-audit-0.1.22.md)。

## 自定义冷却生命周期（0.1.23）

提供者可以登记既有持久字段，共用期限、同步分支后的结果提交和重生复制，不更换原生能力存储。`serverconfig/ars_conflux-server.toml`：

```toml
[addon_cooldowns]
preserve_on_death = true
```

非死亡实体克隆总是保留记录；开关仅控制主动接入此 API 的附属。Iron / Goety 原生冷却保留本体规则。详见 [交接](docs/handoff-cooldown-lifecycle-0.1.23.md)。

## 原生弹体的后续触发（0.1.24）

`ImpactTriggers` 提供短暂的原生伤害成功观察，供提供者决定是否续接剩余 Ars 编排。实体伤害取原生接受结果；方块碰撞与非即时伤害的区域／状态激活另行处理。不会比较生命差值、改变伤害数值或费用，观察结束后才续接，嵌套与异常会恢复外层作用域。详见 [API 约定](docs/projectile-impact-triggers.md)。
