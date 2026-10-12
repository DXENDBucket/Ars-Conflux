# 执行锚点与实体占用

0.1.20 提供中立的 `ExecutionAnchor` 和 `ExecutionClaim`。施法者、damageOwner、account仍由CastContext管理；锚点只表示某次触发实际发生的位置和实时朝向，不在Ars来源附件中跨命中继承。

## 锚点

`ExecutionAnchor.entity(entity, Basis.FEET/EYES, offset)` 随实体移动与转向采样；`position(point, direction)` 冻结精确位置和方向。`sample()` 返回Pose，不移动真实实体，不生成假执行者。附属可在采样后选择水平分量、限制转速、锁定出手朝向或应用嘴部偏移。

`beneficial(context, basis, offset)` 接受自身／Core友军实体或真正的方块命中，拒绝MISS及非生物实体。授权只参照原施法者，通过统一Allies API查询；damageOwner和executor不会自行扩大控制实体的权限。`valid(world, context)` 每次复查实体生命、旁观者、维度、友军关系及锚点所在区块。位置锚点不需要实体授权。附属负责额外的范围、骑乘、着地、地形权限与原生条件。

位置执行的上下文建议 `withExecutor(null)`，实体执行使用实际实体；付款与伤害归属保持不变。会话依然由根施法者存活控制。

上述友军限制是增益便利入口的默认策略，不是所有实体锚点的全局限制。中立的 `present(world)` 只检查实体与位置有效性，`valid(world, Predicate)` 接受附属自己的授权钩子，可用于敌方控制等其他语义。仍需通过会话的权限与付费边界。

## 占用

驱动的 `claims(session)` 返回实际执行实体的声明，每个会话独立持有：

- `ExecutionClaim.action(actor)`：独占ENTITY_ACTION，防止两个物理动作同时修改同一实体。
- `ExecutionClaim.channel(actor)`：共享CHANNELING，允许多个持续输出并存；需要独占引导时可创建exclusive=true的同通道声明。
- 位置输出可返回空列表。是否另外占用根施法者引导由能力显式决定。

不同通道相互独立。`isOccupied(actor, channel)` 用于具体动作冲突，`isCasting(actor)` 查询该实体是否持有任意活跃声明。停止一个会话不会释放其他会话的声明。

Core在运行driver.start之前登记会话，因此嵌套启动也能看到父会话占用。冲突使用既有DENIED结束原因，不新增附属需要映射的枚举。驱动的close仍必须能清理未启动的准备对象，只释放本会话真正获得的外部状态。旧 `occupiesCaster` 默认映射为实际executor的非独占活动声明（没有executor时回退caster），保留旧驱动可并行启动的行为；0.1.21 起该声明使用独立 ACTIVITY 通道，避免旧引导标记被误认为身体行动并遭打断。显式action声明才要求独占。附属应迁移到显式claims，尤其是位置输出与远端实体。

## 可选打断（0.1.21）

`start(session)` 继续拒绝冲突。附属可以显式调用 `startReplacing(session)`：在新会话通过授权及启动付款预留后，以既有DENIED原因结束冲突会话，再启动新驱动。只处理实际claim的冲突，不全局取消同一施法者的所有会话；空claims的位置输出与其他通道不会被删除。启动付款失败或权限无效时旧会话保留。既有按触发付款的repeat模式仍不在创建会话时收费，由附属按原规则检查首次触发条件。

交接时新声明已登记，STARTING期间不能被清理回调或driver.start中的嵌套替换抢走。重复启动同一会话仍返回false且不会取消自己。新驱动在不可预料的启动错误后不会恢复旧动画或外部状态，附属须在交接前完成目标、设备、范围和其他可预检条件；错误路径仍清理新声明，付款事务不会收费。

实体状态的初值（重力、速度等）须在driver.start中、旧close完成后采样。close只有在确实取得相应外部状态时才恢复；准备失败不能解除别人的锁。`isCasting(actor, Predicate<ExecutionClaim>)` 可按声明语义查询忙碌状态，避免施法入口把身体动作占用当成禁止所有后续施法。

## 几何继承

入射方向仍由TriggerGeometry携带。`clear(context)` 或显式写零会清除当前上下文的方向（包括旧Iron键）；null写入表示保留。零向量的scoped调用明确遮蔽外层方向，避免旧入射方向因线程作用域而重新出现。形态转换是否保留之前的入射方向由调用方选择，新命中实体的朝向仍优先于入射方向。

共享测试覆盖provider-only友军、独立伤害归属、实体／位置采样与失去授权、实际actor占用、嵌套冲突、共享引导、逐会话清理及几何重置。原生动画、伤害、费用节拍和客户端绘制均留在附属。
