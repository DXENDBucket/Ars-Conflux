# 主动施法准备与冷却（0.1.9）

Core 提供一次主动使用的边界，不提供任何具体模组的吟唱时间、冷却存储或 GUI。Iron / Goety 可以独立安装，也可以混用；不会互相引用对方的类。

## 注册和责任

在模组初始化时用 `ActiveCasts.register(id, provider)` 登记 `ActiveCastProvider`。每个 provider 只识别自己的魔符，提供 `chantTicks`、原生冷却资格、`released` 回调与 `chantProgress`。查询必须只读；冷却写入只放在 `released`。`released` 接收起始编排及实际发射编排的快照列表；提供者按自己的原生法术／聚晶标识去重。

Core 处理主动工具、附魔弓弩和附魔剑的魔法释放入口。第三方可以显式调用 `ActiveCasts.perform`，或组合 `defer` 与 `execute`；单纯调用 resolver 不自动吟唱或增加冷却。`ActiveCastSource.CASTER_TOOL` 和 `WEAPON` 描述入口用途，不是原生模组枚举。

`perform`：起手检查 → 一次准备 → 释放前检查 → 原 Ars 操作 → 收到有效发射确认后提交所有相关提供者的冷却。`execute` 仅建立主动释放事务，不添加准备时间。`defer` 返回 `UNNEEDED / QUEUED / DENIED`；集成方不得把 `DENIED` 当成即时施法许可。已有有效准备不会被再次点击重置或排队。

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
