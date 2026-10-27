# 主动施法准备与冷却（0.1.11）

Core 提供一次主动使用的边界，不提供任何具体模组的吟唱时间、冷却存储或 GUI。Iron / Goety 可以独立安装，也可以混用；不会互相引用对方的类。

## 注册和责任

在模组初始化时用 `ActiveCasts.register(id, provider)` 登记 `ActiveCastProvider`。每个 provider 只识别自己的魔符，提供 `chantTicks`、原生冷却资格、`released` 回调与 `chantProgress`。查询必须只读；通常在 `released` 提交冷却，依赖实际结果的规则可使用下面的释放后操作。`released` 接收起始编排及实际发射编排的快照列表；提供者按自己的原生法术／聚晶标识去重。

Core 处理主动工具、附魔弓弩和附魔剑的魔法释放入口。第三方可以显式调用 `ActiveCasts.perform`，或组合 `defer` 与 `execute`；单纯调用 resolver 不自动吟唱或增加冷却。`ActiveCastSource.CASTER_TOOL` 和 `WEAPON` 描述入口用途，不是原生模组枚举。

`perform`：起手检查 → 一次准备 → 释放前检查 → 原 Ars 操作 → 收到有效发射确认后提交所有相关提供者的冷却。`execute` 仅建立主动释放事务，不添加准备时间。`defer` 返回 `UNNEEDED / QUEUED / DENIED`；集成方不得把 `DENIED` 当成即时施法许可。已有有效准备不会被再次点击重置或排队。

`afterRelease(caster, key, action)` 可登记依赖执行结果的提交（例如位移确实成功才记冷却）。
同次主动使用按 key 去重，等相关提供者的释放回调完成后才执行；未收到有效发射确认则丢弃。
返回 false 表示当前没有匹配的主动事务，调用者可以立即提交被动／延后命中的结果。
此接口不建立新事务，也不会改变原有被动效果的施法边界；具体提交规则仍属于提供者。

`ars_conflux-server.toml` 的 `[active_casting].chant_mode`：

- `MAXIMUM`（默认）：贡献的最长有效准备时间。
- `SUM`：累加每个书写出现的时间，包括重复魔符，跨提供者合计。散射等运行分支不计入书写出现次数。

提供者独立配置是否贡献吟唱／冷却。Iron 旧的 `chanting.mode=SUM` 保留为该提供者的分组累加覆盖；其默认 MAXIMUM 会逐项提供时间，所以 Core 的 SUM 能正常累加全部 Iron / Goety 出现次数。

## 生命周期和显示

换掉物品、修改物品数据、死亡、离线、切换维度、世界卸载或参与提供者关闭吟唱都会取消。取消不执行操作，也不产生冷却。释放时重新选取目标，费用仍由原 Ars 或触发付费魔符处理。冷却已开始不会取消此前发出的命中、环绕、回响或持续脉冲。

同一主动释放中的嵌套工具入口只复用事务，不会再排队吟唱。自动装置、被动效果和第三方直接 resolver 保留原行为。Ars 正常右键没有已登记魔符时不增加准备或冷却。

`chantProgress` 使用同一 token、总时长与剩余时长通知参与提供者，每秒校正，结束／取消发送 remaining=0。提供者可以把它接到自己的 HUD、移动限制或视觉适配，不得借此改写原生施法状态。Core 的 `ChantProgressTimer` 仅用于客户端显示；旧 token 的结束包不能清除新 token。

## 持续与蓄力边界

准备时长不同于持续执行时长、物品占用时长、弹射物自身预热、连发间隔和松手后的冷却。各法术提供者决定如何映射，不需要完整复制原生交互。新的持续／蓄力映射应另外审查生命周期和冷却提交时机；不得把一次实际触发误当作下一次主动右键。

## 0.1.33 多阶段主动使用

`prepareSequence(phases, caster, hand, source, policy, guard, release)` 对一组预先声明的编排准备一次。传给提供者查询的合并编排只作时间／冷却扫描，不会被当作多形态法术执行或校验。`ActiveCastGuard` 可提供额外存续条件，穿戴施法工具可关闭对无关手持物品的保持要求。

释放回调取得 `ActiveCasts.Sequence` 后，每轮用 `executeSequence(use, phase, action, denied)` 包裹原生 dispatch。首个被 resolver 确认的释放为整个声明计划提交一次起手冷却；后续轮次不重新准备或复查本次起手冷却，但保留各效果的费用和原生资格判定。调用者须保持声明编排不变，并在松开、换装、死亡、离线、换维度、卸载和异常时 `close()`；取消前已提交的冷却不会撤回。

准备成功不等于释放成功；只有实际 resolver 确认才承诺激活。准备期取消不会执行结束法术。本 API 不规定开始／持续／结束三阶段，也不引用 Zero；其他持续物品可自行划分阶段。Zero 的具体接线与差异见 [可选适配说明](ars-zero-compatibility.md)。

## 0.1.22 公共反馈与入口策略

提供者可返回 `CastRejection`：只查询、不扣费、不自行发消息；Core 选择最长已知剩余冷却，以统一 actionbar 显示魔符名称和秒数。原有 `allowed` 实现仍兼容，不强迫旧提供者重写，但其自行提示不受统一展示控制。冷却仍由提供者保存，Core 不维护第二套期限。

`ActiveCasts.perform` 增加接受 `ActiveCastPolicy` 的兼容重载，可独立绕过准备或冷却。所有旧入口保持原配置；关闭冷却不清除已有期限，不改变资源支付。`ActiveCastPolicy.attach` 让不可变策略跟随 Ars 上下文克隆；持续会话中的结果冷却应调用 `ActiveCasts.cooldownsEnabled(caster)`，不能脱离该会话的上下文自行提交。

`afterRelease` 专用于结果冷却提交；本次入口禁用冷却时，返回 true 并丢弃该提交，防止调用方退回即时写入。普通被动触发仍不创建主动事务。

`MovementLimits.multiplier(event, requested)` 合并同一客户端输入事件的多个限制，以最严格的倍率为准；返回增量倍率，调用方只乘一次。事件对象仅作为弱引用键，API 不依赖某个平台客户端事件类。

## 0.1.23 结果冷却与持久记录

`PersistentCooldowns.register(id, legacyNbtKey)` 返回独立名称空间，读取旧字段，用主世界时钟维护期限。`extend` 只延长；`commit` 用 `afterRelease` 延后本次同步分支的结果提交，失败释放不提交，后续独立命中即时提交。相同项不同期限最终取较长值。该接口不自行检查创造、是否启用或原生能力资格；调用方保留这些规则。

登记应在附属启动时完成。Core 为已登记记录统一处理 Player Clone，默认死亡后保留；非死亡克隆始终保留。配置 `[addon_cooldowns].preserve_on_death = true` 仅影响选择此存储的附属，不改变 Iron / Goety 原生能力字段。

非体术效果允许并行，持续状态只用于展示/清理，不应作为新的主动释放障碍。体术模拟仍用 `ExecutionClaim.action` 支持替换；吐息的 shared channel 和旧 ACTIVITY 均非独占。每次执行继续独立支付费用、复查目标资格。
