# 0.1.34

- Fix Ars Zero 2.0.2 staff/circlet glyph-category placement: start each type on its own row with the native category icon at the left.
- Paginate the actual grid instead of slicing 70 glyphs before accounting for headings; keep all glyphs reachable through arrows, wheel scrolling and search.
- Retain actual Ars/addon category predicates and Conflux provider order; deduplicate known/default starting glyphs and remove previous-page widgets completely.
- Client-only optional Zero fix, under the existing native-fixes switch. Targeted layout and released-bytecode checks pass; no client session launched for verification.

# 0.1.33

- Add provider-neutral multi-dispatch active uses: prepare the declared plan once and acknowledge activation cooldowns once, while preserving per-trigger resources and native effect eligibility.
- Add optional, version-gated Ars Zero 2.0.2 staff/circlet integration on NeoForge 1.21.1, and exclude dispatch-owned costs from its beam repeater quote.
- Apply runtime block-capture safety, Select height and bounded Convergence source-accounting fixes without replacing the official Zero jar.
- Make temporal targeting read the preceding actual execution, with separate input/output, stale-callback isolation and complete anchor/block-group cleanup. Native effects and visual helpers remain in use.
- Add low-memory policy and released-bytecode checks. No game/client session is claimed by these checks. Forge shares the neutral API only; Zero integration is NeoForge-only in this release.

# 0.1.29

- Integrate Ars 'n Spells 3.4.0 exported Ars scrolls and inscribed items with the shared preparation and cooldown boundary on both loaders.
- Prepare native Iron wheel proxy casts before native initiation, then acknowledge the real release inside ANS's existing payment lifecycle. Cancelled or unpaid casts start no provider cooldown.
- Retain ANS targeting, school selection, pricing, native proxy cooldown and carrier creation. Native Iron spells and installations without a supported ANS entrypoint remain unchanged.

# 0.1.28

- 修复 Forge 1.20.1 普通／无限法术书按固定数量切页造成的遗漏与跨分类重叠，按实际网格和分类标题空间分页。
- 魔符按注册 ID 去重；搜索和前后翻页使用实际页数，保留 Ars 与附属的分类、排序及原生编排验证。
- NeoForge 1.21.1 继续使用既有 GlyphFormatter 分页修复，不替换其书页实现。

# 0.1.27

- 修复已有动作级错误被复用时，另一个不合法增强仍显示为亮的边界情况。
- 用保留形态和载荷的独立候选对照区分新增问题与旧问题，合法无关增强保持可选。
- 增加真实客户端注册表亮灭审计：增强适用性、次数上限、跨附属载荷互斥、效果分段和法术书等级限制；保留原错误及增强说明。

# 0.1.26

- 周围友军使用统一的原版 Ars 形态外框、中心缩放与客户端配色。
- 修复错误锚定在前方动作时，候选魔符仍错误显示为亮的问题，包含重复弹射物替换及需载荷的增强。
- 仅调整书页追加预览；已有无关错误不会污染候选判断，施法验证原位置不变，原生错误/增强文本保留。
- 预览刷新内缓存前缀验证，避免逐个候选重复检查同一前缀。

# 0.1.25

- 新增客户端魔符图标配置与 `GlyphVisuals` / `GlyphIcon` 中立 API。
- 运行时组合 Ars 原版底板和分类外框，支持中心缩放、统一/自定义颜色及原生三维物品；不打包原模组素材。
- 保留无效编排淡化与中心原生纹理，支持资源包重载；普通 Ars 图标与手持外观不变。

# 0.1.23

- 新增 opt-in `PersistentCooldowns`：共用主世界期限、同步释放后的结果提交与 Clone 复制；保留提供者原字段、时长和资格。
- 自定义冷却默认死亡后保留，提供独立开关；不改变 Iron / Goety 原生存储。
- 明确非体术效果允许并行；相关附属修正绑定工具忙碌与海灵光环提交/持续阻塞。

# 0.1.22

- 共同冷却拒绝结果与最长剩余时间提示，保留原生期限与创造模式策略。
- 输入事件的移动限制合成 API，避免 Iron / Goety 混合吟唱重复减速。
- Hex-Ars Linker 三种主动入口接入吟唱/冷却；两个独立配置默认开启，被动回调不重复起手。
- 中立 `ActiveCastPolicy` 随上下文继承，不改变 Mana / 媒质计费。
- Hex 可选钩子兼容 Forge 自带 MixinExtras 0.4.1；无 Hex 前置时不链接其类型。

# Changelog

## 0.1.17

- NeoForge: fix Ars book category headers overflowing off the page when row padding crosses the 98-slot boundary, notably with large addon augment lists. Preserve native widgets and provider sorting.
- Add boundary, multi-page and unchanged-grid regression checks. Forge keeps the same release number; Ars 4 uses a different book layout and does not contain this formatter.

## 0.1.16

- Add a neutral, extensible alliance-query API using native team and ownership relations.
- Add the optional Tier 3 Nearby Allies cast method with independent target continuations, AOE radius and preserved attribution.
- Add a transparent pixel icon, bilingual text, native resolve particles and conditional Scribe recipes on NeoForge and Forge.
- Default Nearby Allies and NeoForge Elemental Phalanx registration to off; require matching startup configuration.
- Add directed registry, ownership, fanout and context-preservation tests.


## 0.1.15

- Add a provider-neutral remote execution world and same-instance managed projectile migration. Preserve carrier state, visual/native session state, attribution, payment account and origin-clock lifetime; unload of either world and loss of the original owner still cancel the execution. Do not capture independent impact-created projectiles.
- Adapt Controle's cross-dimensional Warping Spell Prism on both platforms. Managed carriers stay off-world instead of spawning a duplicate Ars projectile; plain Ars retains upstream transfer behavior, source costs and chunk-loading rules.
- Fix Additions Retaliate on both platforms: damage callbacks select the current source attacker before vanilla writes damage history. Manual uses keep upstream targeting; unattributed damage does not select a stale attacker.
- NeoForge only: snapshot both random operands of Controle binary filters, using each operand's own augments and upstream probability formula. Forge Controle 1.1.1 has no filters; none are ported.
- Add opt-in real-addon integration fixtures, including native prism hits, transfer rejection/cleanup, frozen continuations, reactive targets and random operand isolation. Optional addons are not embedded or required.

## 0.1.14

- Add NeoForge startup option `optional_content.enable_elemental_phalanx`, enabled by default. Register Elemental's native Carian Phalanx in production when installed; disabling preserves upstream registration. Conflux loads before Elemental so the real startup file is read before glyph/item registration.
- Add a conditional Ars Scribe's Table recipe using a homing projectile glyph, manipulation essence, conjuration essence and an amethyst shard. Keep upstream ID, tier, name, models and mechanics; no artwork is copied.
- Add test-only production-gate simulation to check actual registry and recipe availability with the startup option on and off, rather than relying on upstream development registration.
- Keep Forge 1.20.1 at the same release number; its Elemental does not implement Phalanx, and no startup activation is added there.

## 0.1.13

- Extend the optional generated-carrier budget hook to Ars Elemental's arc and homing propagators, including when NEG delegates those glyphs to Elemental. Preserve native emission layout and explicitly bind child carrier provenance on both platforms.
- Register Elemental's Carian Phalanx as a driven, once-paid projectile carrier. Preserve its native formation slots, enemy detection, launch, homing, hits and cleanup while using provider replacement visuals; bind the child resolver after Elemental strips the original form.
- Availability caveat: Elemental 0.7.10.4 registers Carian Phalanx only in development environments. This is preparatory compatibility for that implementation; Conflux does not enable it in production.
- Add opt-in real-Elemental integration fixtures for generated fans, empty accounts, plain-addon emissions, and NeoForge phalanx lifecycle/source continuity. No Elemental classes, artwork or dependencies are included in release JARs.

## 0.1.12

- Preserve NEG SpellBinder focus-thread resolver factories, source extensions and native quotes across derived branches and replacement collision wrappers.
- Budget NEG's generated projectile, arc, homing and missile fans within the child emission. Reduce count before NEG computes its native fan; ordinary Ars/NEG spells keep their original count and resource rules.
- Add public `CarrierRegistry.bind(resolver, glyphId)` and `of(resolver)` for generated carrier provenance. This also fixes Ars 4/NEG propagators whose synthetic recipes do not identify arc or homing carriers.
- Keep all NEG adapters optional. Share the implementation between NeoForge 1.21.1 and Forge 1.20.1; provider mods need no NEG-specific patches.
- Add opt-in real-NEG integration fixtures for resolver/focus retention, chained/filtered/plane effects, generated fans, and NeoForge trail/contingency continuations. See `docs/not-enough-glyphs.md` for scope and commands.

## 0.1.11

- Add public `ActiveCasts.afterRelease(caster, key, action)` for result-dependent commits after an acknowledged active release. Keys deduplicate callbacks within a use; failed/unacknowledged uses drop them, while passive callers can commit immediately when the method returns false.
- Share the API between NeoForge 1.21.1 and Forge 1.20.1. No provider-specific cooldown policy is added to Core.

## 0.1.10

统一提供者注册、主动准备、取消和释放后的冷却提交；混合编排只吟唱一次，保留原生 HUD 回调。

## 0.1.8

- Add opt-in `GlyphPresentation` registration: keep semantic categories, place participating providers after ordinary Ars/addon glyphs and keep each provider contiguous.
- Centralize Ars 4 registry comparators and Ars 5 spellbook category sorting; preserve specialized optional-addon category predicates without linking their classes.
- Order providers by namespace independently of initialization order; retain provider-specific secondary keys such as native Iron schools.
- Transfer Iron sorting hooks and the same-rank effect/subform regression checks into Core.

## 0.1.7

- 新增 `ArsSpellAccess.level(context)` / `caster(context)`：安全读取原始世界；服务端沿用 Ars 的施法者逻辑，客户端及未绑定上下文只读取已保存实体，允许为空。
- 统一施法上下文使用安全入口，避免客户端装置、网络实体或未绑定上下文创建服务端假玩家。
- Forge 起手支付先检查上下文世界；散射预算及原版效果触发计费仅在服务端执行。客户端原生回调继续通过。
- 无法取得载荷报价时保留原散射数量，不将未知报价当作免费账户。
- Iron & Nouveau 0.17.6 接入同一 API；补充端侧审查记录和双版本定向回归。
- 排查发现 Iron／Goety 的 Forge 源目录开发启动缺少 Core Mixin 配置；补齐两个开发构建的启动参数，发行包已有声明。

## 0.1.6

- 弹射物转换入口在读取 Ars 解析器或施法者之前检查服务端世界，客户端网络实体按原样加入，不发起原生替换或计费。
- 修复客户端弹射物上下文调用 `getUnwrappedCaster()` 时将 `ClientLevel` 强转为 `ServerLevel` 的同类异常；原始用户报告来自 Iron & Nouveau 0.16.2。

## 0.1.5

- 新增公共 `ResolverExtension` / `ResolverContexts`，让入口来源语义随解析器派生、共享轨迹碰撞包装和原生报价继续传递；不覆盖提供者自定义解析器行为。
- Core 内部集中维护可选 Linker 适配，保留 Hex 环境、原生回调深度与递归限制；未安装 Hex 时不加载其类型。
- 可选 Ars 触发计费保留 Linker Mana 倍率；起手省略的人工形态费用不再从命中效果重复扣除，额外媒质仍按完整原生术式报价。
- 修复 Ars 5 中 Linker 的资格报价与实际扣款倍率不一致；默认关闭 Core 的原生触发计费开关。
- 新增提供者首次费用 API `TriggerPaidGlyph.firstCharge` 与 `SpellResourceQuotes`，按物理资源池合并检查，未知费用不视为免费。Iron 0.17.4 提供通用报价，不包含 Hex 专用逻辑。
- 双版本定向验证覆盖真实 Ars → Hex VM → Ars 回调、实际媒质支付、指定方块、代施法付款归属、两种计费模式、倍率、空蓝、创造免费和递归限制。
- 保留 Linker 的原生学习与启迪要求；Forge 测试使用世界生成的代施法图案并为测试玩家授予启迪。没有新增 Hex 查询图案或异步吟唱等待。

## 0.1.4

- 将 Hex-Ars Linker 的可选适配声明、责任边界和独立集成验证集中到 Core。
- 常规发射、Linker 自建的指定位置弹射物、指定实体效果共用既有公共调度和施法上下文，不添加 Hex 专用运行时类型依赖。
- 两个独立测试提供者检查真实 Linker 入口、归属／方向、克隆、单次转换和资源不足；无需安装 Iron 或 Goety。
- 为双版本测试提供可选依赖清单、SHA-256 校验下载工具；测试依赖与测试提供者不进入发行包。
- 保持原版 Ars 默认计费；可选原生触发计费与 Linker 媒质报价的组合仍未验证，Hex 程序的异步吟唱等待不在此次范围内。

## 0.1.3

- 新增公开 API `ArsSpellAccess`：不可变法术部件快照、弹射物解析器读写和法术箭解析器读取。
- Ars 4 / Ars 5 的字段与方法差异仍封装在 Core 平台实现中，附属可以共享调用代码。
- Iron & Nouveau 0.17.2 用该 API 合并双版本源码；Core 仍不依赖 Iron，不改变原版默认计费或生存流程。

## 0.1.2

- 接入统一施法上下文：施法者、实际执行者、伤害归属、支付账户、目标及命中方向分别传递。
- 使用 Ars 既有上下文附件传递不可变归属，克隆与常规延迟保留账户；新命中不继承上一次代理执行者。
- 公共会话恢复上下文作用域，并在实际执行实体死亡、移除或跨维度后终止；保留旧施法者构造入口。
- Iron & Nouveau 0.17.1 两端接入效果、弹射物接续、吐息／射线、持续触发和动作效果。
- 保留既有资源数值、原生法术控制及配置；没有增加独立性能保护或通用召唤物规则。

## 0.1.1

- 增加独立的服务端配置 `ars_trigger_billing.enabled`，默认 false，原版计费行为保持不变。
- 开启后将 Ars 原版效果与书写增强费用从起手报价移到各次效果执行，起手资格只检查剩余起手费用。
- 原版装备减耗按费用份额分配，整数舍入守恒；计费事件和原生支付入口继续使用 Ars 的接口。
- 嵌套环绕每次创建子环绕均须付费，空蓝的分支不再产生内层球体；重复命中逐次付费。
- 创造模式免费，附属自有计费与非生物装置不受这项配置影响。
- 在上下文保存本次施法的计费模式，配置改变不会让飞行中的法术重复收费或变为免费。
- 两端增加原生 Ars 独立定向 GameTest；补充费用分配单元测试与配置说明。
- 补齐资源包元数据，Forge 独立加载时能正确读取资源和测试结构。

## 0.1.0

- 从 Iron & Nouveau 抽出独立的共享执行核心，两端共用公共源代码和版本号。
- 提供通用资源账户、共享物理池预留和触发成功提交 API；提供公开的资源池身份。
- 提供执行会话、作用域、命中接续和生命周期调度，不引用 Iron 原生类型。
- 提供可注册载体、替换载荷、共享 Ars 轨迹和对称散射预算。
- 集中 Ars、NEG 与 Ars Elemental 的公共载体钩子、触发方向与组合验证。
- Iron & Nouveau 0.17.0 接入，两端魔符、配置和玩家学习数据 ID 保持原值。
- NeoForge：4 项资源单元测试、4 项不安装 Iron/NEG 的 Core GameTest、7 项 Iron 迁移 GameTest 已通过。
- Forge：发行构建、重映射及 4 项 Iron 迁移 GameTest 已通过。
- 两端各增加 1 项独立提供者边界验证：第三方载荷可接 Iron 效果，组合规则不要求双方专门适配。
- 原版 Ars 默认计费未改动；Goety 接入尚未实现。
