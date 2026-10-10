# Changelog

## 0.1.9

- Add provider-neutral `CastInput`, `InteractiveDriver`, `InputBinding`, and `InputSession` APIs on the existing execution lifecycle. Core defines no native spell IDs, point counts, ranges, prices or cooldowns.
- Route subsequent active Ars 4/5 casts to the matching live endpoint; rejected input is consumed without advancing, sneak activation cancels, and source/recipe changes close stale state. Ordinary casts without an endpoint retain their behavior.
- Add renewable execution deadlines and one-shot `ArsContinuation` snapshots at the current cursor, so resuming does not restart preceding glyphs or active-cast billing. Preserve provider execution scopes.
- Revalidate bindings and permissions before delayed completion; reject expired input rather than renewing it. One interactive input owner per actor, with existing background sessions unaffected.

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
