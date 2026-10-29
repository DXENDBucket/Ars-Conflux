# 轨迹载体的碰撞与存续

0.1.39 新增显式启用的 `ArsTrajectory.contactPolicy`、`TrajectoryContacts` 与 `TrajectoryContact`。Core 处理形态碰撞边界，不引用 Iron、Goety 或其他能力提供者的类。旧 `ArsTrajectory` 构造和 start 签名不变，未启用策略的附属维持原行为。

## 轨迹与存续

```java
var motion = new ArsTrajectory(nativeVisual, originalCarrier)
    .contactPolicy(contact -> contact.hit() instanceof EntityHitResult);
```

必须在 start 前声明。返回 true 仅阻止该次接触中的 Ars `attemptRemoval` 消耗和删除；继续运行同一个原始载体，保留速度、年龄、重力、形态状态及其正常更新。没有复活已删除实体、重置年龄或设置无限穿透。

自然到期、主动移除、卸载或会话关闭仍按原有路径执行。提供者负责其原生可见实体是否需要委托寿命，不能让原生 tick 再额外推进运动或碰撞。

## 接触与目标展开

`TrajectoryContacts.collide(carrier, physicalHit, action)` 包裹一次真实碰撞。`pulse(carrier, action)` 包裹一次形态脉冲，例如回响轨迹或溅射弹到期时的最终激活。后者不授权保留过期载体。嵌套的同载体范围展开复用外层接触，其他载体有独立帧；异常结束也恢复之前的上下文。

已有入口覆盖 Ars 的 traceAnyHit 虚派发与基本 onHit、Orbit 的覆盖方法，以及 NEG 的 Missile/Trailing 范围脉冲。新的形态若完全绕过这些入口，可调用以上中立 API；无需各能力附属再对该形态制作兼容。

在原有 start 回调中读取：

```java
var contact = TrajectoryContacts.current(carrier);
if (contact == null) {
    activate(() -> resolveTarget(hit));
} else {
    contact.activate(this::activate, () -> resolveTarget(hit));
}
```

`activate` 接受既有 `TriggerPayment`。首个成功目标提交一次付款，其余范围目标执行同一接触内的作用；失败按原事务退款，后续目标仍可尝试。目标后续编排本身的费用继续独立计算。帧只在当前同步接触中有效，不能保存给以后 tick 使用。

形态范围里的多个 EntityHitResult 不等于多次真实碰撞。Core 不把溅射目标再次扩张成一轮新溅射，也不把付款与伤害成功、穿透预算或寿命混为同一标志。

## 提供者职责

- 声明哪些原生接触应保留，以及有限穿透等能力的实际规则。默认继续使用载体规则，Core 不猜测外部实体的移除理由。
- 维护原生接触间隔、状态、友军、伤害归属和命中成功语义。
- 连接首次已付费与后续触发付费；Core 仅提供一轮目标展开共享付款的边界。
- 当该能力采用形态寿命时，避免其原生可见实体提前按自由飞行寿命消失。

首个接入是 Iron & Nouveau 0.17.30 的闪电球。其他提供者未被自动改写。

## 验证与版本边界

本批以 Iron 闪电球作为接入样例，NeoForge 通过 5 项、Forge 通过 3 项定向 GameTest。1.20.1 的 NEG 3.2.1.1 没有新版 Missile/Splash，旧版验证 Orbit，未宣称旧版具有该形态。Forge 最终重混淆发行包已与 Iron、Ars、NEG 启动到主菜单；客户端施法画面尚未人工验收。详见 [构建记录](trajectory-contacts-build.json)。

隐藏载体也排除自身的可见代理，避免将同一颗逻辑弹体的显示实体当作碰撞目标。未启用接触策略的弹体不获得新的保留或收费规则。
