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

0.1.5 起，已自行计费的魔符可实现 `TriggerPaidGlyph.firstCharge(context, augments)`，返回一次首次触发的 `ResourceCharge`。`ProjectileForm` 默认复用 `initialCharge`。报价必须只读，沿用实际等级、工具和账户，不改变学习进度、余额或世界状态；无法报价时返回 null，只有确定免费才能返回金额 0。

```java
var estimate = SpellResourceQuotes.estimate(context);
boolean allPricesKnown = estimate.complete();
boolean canCoverKnownPrices = estimate.knownAffordable();
boolean canCoverEstimate = estimate.affordable();
```

预估保留每个提供者魔符的条目，并按相等的 `reservationKey()` 合计同池费用。效果读取紧随其后的书写增强；替换形态读取同一增强段中前后的其他增强。它只估计每个魔符可能发生的一次首次激活，未含散射数量、持续脉冲、重复命中、原生 Ars 起手费或 Hex 媒质费，不是整个法术总费用的上界，也不是权限或目标有效性判断。未知条目会使 `complete()` 和 `affordable()` 返回 false；`knownAffordable()` 只检查可知部分。实际触发始终重新报价和预留。

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

第三方 Ars 入口的共享适配由 Core 维护，例如 [Hex-Ars Linker](hex-ars-linker.md)。法术提供者只实现公共载荷、效果与资源接口，不检测 Hex 解析器类型，也不复制 Hex 或 NEG 的生成钩子。以后入口改变或出现新的通用载体时，在 Core 补充一次适配；具体原生法术的实体和行为仍由相应提供者实现。

## 编排与空间

0.1.7 的 `ArsSpellAccess.level(context)` 直接读取存储的世界，不查询施法者；未绑定上下文可以返回 null。`caster(context)` 在服务端沿用 Ars 原有行为，包括装置的原生假玩家回退；在客户端或缺失世界时只读取存储的实体，装置／网络上下文可能返回 null。`CastContext.of` 也使用这个安全入口。调用方仍须在执行世界变化、资源支付及权限事件之前检查服务端，安全读取本身不代表可以执行法术。

`ArsSpellAccess.parts(spell)` 返回不可修改的部件列表快照，避免附属直接依赖 Ars 4 的可变列表字段或 Ars 5 的读取方法。它保留原有顺序和部件实例；附属不能通过该列表改写原术式。

`ArsSpellAccess.resolver(projectile)` 读取 `EntityProjectileSpell` 的解析器，同名双参数方法设置解析器；法术箭 `EntitySpellArrow` 有独立的读取重载。法术箭不是 `EntityProjectileSpell` 的子类。这些 API 在两个平台具有相同源码签名，仍需分别编译发行包；不要引用 Core 内部的 `platform.ArsAccess`。

`SpellValidators.register(providerId, rule)` 让不同附属贡献组合规则。Core 自带“同一形态段只能有一个替换载荷”规则，避免跨附属同时接管一枚弹射物。

提供者应只检查自己拥有的载荷。如果形态段已由其他 `ProjectileForm` 接管，不要再要求它携带自己模组的载荷；各家的效果仍可在后续编排中组合。识别公共接口即可，不需要引用另一家模组的实现类。

`TriggerGeometry` 在 Ars 上下文保存方向，并在命中／接续作用域恢复。新写入的 key 为 `ars_conflux_incoming`，旧 Iron 方向字段仍可读取。`CastContext` 显式区分 caster、executor、damageOwner、target 和 account；`withExecutor` 不会把伤害与费用转给被选中的实体。

0.1.2 起，`CastContexts.bind` 通过 Ars 原有附件传递不可变的施法归属和账户，克隆、子上下文及常规延迟可保留；目标和执行者则在每次触发时重新确定。会话接受完整上下文，并在启动、tick、命中与清理时恢复作用域。Iron 的效果、弹射物、代理动作及持续施法已接入。详细约定见 [统一上下文](cast-context.md)。

`TriggerPaidGlyph` 是附属已经自行实现触发计费的声明，主要让 Ars 的剩余起手费用在分支执行前支付。其可选 `firstCharge` 提供只读报价；声明本身不会取消该魔符的原生起手价格，也不推断效果是否成功。

## 解析器来源

`ResolverExtension` 使用 Ars 的不可变上下文附件补充特殊施法入口的环境与报价语义，与 `CastContext` 中的施法归属、空间和账户分工独立。入口用 `ResolverContexts.attach(resolver, extension)` 附加它。克隆沿用 Ars 附件复制；Core 的 `getNewResolver` 钩子和 `ArsTrajectory` 碰撞包装传递附件。

`derive(source, proposed)` 可在 Ars 提议普通子解析器时恢复入口自定义解析器；已有的碰撞或提供者解析器应保留自己的执行行为。`quote(source, context)` 可返回专用报价解析器，否则返回 null；同一来源最多一个附件拥有报价工厂。`ResolverContexts.quote` 用于需要保留原生入口定价的内部报价。

自建包装器可以使用 `ResolverContexts.inherit(source, wrapper)` 仅传递来源附件。自行覆盖 `getNewResolver` 的提供者也应调用该方法，或明确调用 `derived`；普通基类的 mixin 不会自动拦截独立覆盖方法。提供者使用 `ResolverExtension` 时不应引用 Core 内部的 Hex 桥。附件是临时运行状态，不序列化外部 VM，也不让多个独立施法共享可变上下文。

## 原版 Ars 的可选接入

0.1.1 提供独立的 `ars_trigger_billing.enabled`，默认关闭。适配器同步改变起手费用、装备减耗份额及施放资格，并在原版效果入口通过同一套公共事务按次收费。环绕创建也是原版效果，因此嵌套分支不能绕过支付。

这不是提供者的通用成功判定：原版 `onResolve` 返回 void，正常返回只代表完成了一次付费尝试。效果内部的多目标操作与自动持续状态不拆成额外事务。非生物装置保留原有支付规则。具体语义和配置见 [原版触发计费](trigger-billing.md)。

## 魔符展示排序（0.1.8）

在模组初始化时调用 `GlyphPresentation.register(namespace)`，或传入本模组内部的次级排序键 `Function<AbstractSpellPart, String>`。注册命名空间按稳定字典序聚在同类普通 Ars／附属魔符之后；没有注册的魔符继续采用上游的内部顺序。内部排序键只描述本模组，不需要辨认其他提供者。不允许重复覆盖同一命名空间。

Core 管理 Ars 4 注册表比较器和 Ars 5 法术书的类别分组。Ars 5 使用实际注册的分类谓词，保留可选附属的特殊类别；不会通过更改 `getTypeIndex()` 把效果挪成子形态。比较器在执行时读取登记表，避免模组初始化先后改变结果。没有提供者登记时保留原生比较器行为。

## 主动施法（0.1.9）

`ActiveCasts` 统一已注册提供者的主动准备、取消与释放事务；`ActiveCastProvider` 保留原生数值、冷却存储和显示责任。详见 [主动施法](active-casting.md)。
