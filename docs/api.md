# API 与责任边界（0.1）

稳定性承诺覆盖 `dev.arsconflux.api`；`platform`、`mixin` 和模组初始化类属于内部实现。0.x 阶段仍允许在次版本中调整公共 API。Minecraft、Ars 版本差异的内部访问放在平台目录，两端共用公共源代码。

## 触发资源

`ResourceAccount` 是物理资源池的适配器。`ResourceCharge` 表示本次触发的费用，`ResourceTransactions` 处理预留与提交。费用单位是提供者定义的非负整数，不强制要求是 Mana。

```java
ResourceAccount account = new ArsManaAccount(context.getCaster());
ResourceCharge charge = new ResourceCharge(account, 30);
boolean applied = charge.trigger(() -> applySpellEffect());
```

`applySpellEffect()` 返回成功才扣款。余额不足时不会调用效果。异常和失败释放预留，但不撤销调用方已经产生的世界变化。若内层触发已经成功付费，外层后来失败也不会退回内层费用。

所有访问同一个物理池的适配器必须使用相等的 `reservationKey()`。可以使用公共的 `ResourcePoolKey.of("ars_nouveau:mana", poolOwner)`，不要把支付附属的 ID 当作资源种类。玩家与生物都可以是池所有者，装置可用自己的稳定身份。开启跨模组魔力统一时，联动提供者还需要把别名映射到同一个实际池；Core 不猜测其他模组的统一配置。

`ArsManaAccount` 默认按 LivingCaster 的实体共享预留，创造玩家免费。NeoForge 的装置包装器可沿用 Ars 5 的支付接口；Forge Ars 4 没有该包装器接口，非生物装置必须提供自己的 `ResourceAccount`。Core 不创建假玩家，也不把所有装置费用自动转嫁给某位玩家。

`charge.affordableCount(requested)` 返回当前可支付次数。它只计算预算，不扣款；每次执行仍要通过 `trigger` 预留和提交。调用必须在同步服务端执行范围内完成，不能把事务跨 tick 挂起。

## 会话

`ExecutionSession<P>` 保存提供者自己的参数快照 `P`，调用 `ExecutionDriver<P>` 的 `start/tick/close`，由 `ExecutionSessions` 统一调度。

```java
var session = new ExecutionSession<>(world, caster, parameters, 100,
        driver, continuation, permissionCheck, ExecutionScope.DIRECT);
session.billing(TriggerPayment.of(charge), false);
boolean started = ExecutionSessions.start(session);
```

`repeat = false` 只在成功启动时付费；`repeat = true` 由 driver 调用 `session.activate(action)` 对每轮实际激活付费。间隔、首轮、空蓝后等待还是终止都由提供者定义，Core 不擅自统一不同法术的规则。

`ExecutionScope` 恢复附属自己的临时上下文。driver 需要在全部结束路径清理实体；Core 确保只进行一次终结，并在 driver 的清理抛异常时仍关闭 continuation。`occupiesCaster()` 表示该执行正在占用施法者，可以支持吐息等状态查询，不要求识别 Iron 的具体实现类。

会话是临时运行状态，不是世界存档格式。长期召唤物、领域、跨区块持久化仍由法术附属管理。Core 上限为 4096 个活动会话，触顶会拒绝新的执行并走清理路径。

## 弹射物

在 Ars 强化魔符上实现 `ProjectileForm`，提供：

- `replace(carrier, profile)`：接管成功或已识别但执行失败均返回 `true`，原 Ars 实体不再入世；不适用时返回 `false`。
- `initialCharge(context, augments)`：返回单发实际费用，供对称散射在生成之前计算数量。

`CarrierRegistry.register(CarrierProfile)` 允许新增载体声明，不需要修改一个封闭枚举。`arsDriven` 表示保留 Ars 轨迹，`repeated` 表示后续可反复触发，`emitted` 描述调态节点实际发出的形态。

对于复杂载体，`ArsTrajectory` 让原 Ars 实体在世界外继续计算运动和命中，实际入世的替换实体提供外观与载荷。它可以接受任意 `Entity`，并不引用 Iron 类。接入者必须抑制替换实体自己的重复运动与碰撞，设置原生 owner、粒子、载荷及伤害回调；不能同时把两个实体都加入世界。

`CarrierProxy` 让回响轨迹的目标查询排除自己的替换外观。载体须遵守 Ars 的 resolver 命中契约；完全绕过 resolver 的附属仍需专门适配。

## 编排与空间

`SpellValidators.register(providerId, rule)` 让不同附属贡献组合规则。Core 自带“同一形态段只能有一个替换载荷”规则，避免跨附属同时接管一枚弹射物。

提供者应只检查自己拥有的载荷。如果形态段已由其他 `ProjectileForm` 接管，不要再要求它携带自己模组的载荷；各家的效果仍可在后续编排中组合。识别公共接口即可，不需要引用另一家模组的实现类。

`TriggerGeometry` 在 Ars 上下文保存方向，并在命中／接续作用域恢复。新写入的 key 为 `ars_conflux_incoming`，旧 Iron 方向字段仍可读取。`CastContext` 显式区分 caster、executor、damageOwner、target 和 account；`withExecutor` 不会把伤害与费用转给被选中的实体。

`TriggerPaidGlyph` 是附属已经自行实现触发计费的声明，主要让 Ars 的剩余起手费用在分支执行前支付。它不负责计算价格、取消该魔符的原生起手价格，或推断效果是否成功。

## 原版 Ars 的后续接入

公共事务已可用于原版效果适配，但 0.1 没有启用完整的原版触发计费模式。后续模式应显式选择需要移至触发阶段的费用，同步更新起手总价与施放资格检查；形态创建、目标效果和持续脉冲分别接入，定义好 miss、空目标、失败和部分成功的语义。原版多数效果没有成功返回值，不能简单在任何 `onResolve` 返回后都视为成功。

计费规则应可选，默认保持 Ars 行为。外部资源池适配、效果价格与失败策略都不能固定成 Iron 专属规则。
