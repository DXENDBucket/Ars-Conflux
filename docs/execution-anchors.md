# 执行锚点与实体占用

0.1.20 提供中立的 `ExecutionAnchor` 和 `ExecutionClaim`。施法者、damageOwner、account仍由CastContext管理；锚点只表示某次触发实际发生的位置和实时朝向，不在Ars来源附件中跨命中继承。

## 锚点

`ExecutionAnchor.entity(entity, Basis.FEET/EYES, offset)` 随实体移动与转向采样；`position(point, direction)` 冻结精确位置和方向。`sample()` 返回Pose，不移动真实实体，不生成假执行者。附属可在采样后选择水平分量、限制转速、锁定出手朝向或应用嘴部偏移。

`beneficial(context, basis, offset)` 接受自身／Core友军实体或真正的方块命中，拒绝MISS及非生物实体。授权只参照原施法者，通过统一Allies API查询；damageOwner和executor不会自行扩大控制实体的权限。`valid(world, context)` 每次复查实体生命、旁观者、维度、友军关系及锚点所在区块。位置锚点不需要实体授权。附属负责额外的范围、骑乘、着地、地形权限与原生条件。

位置执行的上下文建议 `withExecutor(null)`，实体执行使用实际实体；付款与伤害归属保持不变。会话依然由根施法者存活控制。

## 占用

驱动的 `claims(session)` 返回实际执行实体的声明，每个会话独立持有：

- `ExecutionClaim.action(actor)`：独占ENTITY_ACTION，防止两个物理动作同时修改同一实体。
- `ExecutionClaim.channel(actor)`：共享CHANNELING，允许多个持续输出并存；需要独占引导时可创建exclusive=true的同通道声明。
- 位置输出可返回空列表。是否另外占用根施法者引导由能力显式决定。

不同通道相互独立。`isOccupied(actor, channel)` 用于具体动作冲突，`isCasting(actor)` 查询该实体是否持有任意活跃声明。停止一个会话不会释放其他会话的声明。

Core在运行driver.start之前登记会话，因此嵌套启动也能看到父会话占用。冲突使用既有DENIED结束原因，不新增附属需要映射的枚举。驱动的close仍必须能清理未启动的准备对象，只释放本会话真正获得的外部状态。旧 `occupiesCaster` 默认映射为实际executor的非独占活动声明（没有executor时回退caster），保留旧驱动可并行启动的行为；显式action声明才要求独占。附属应迁移到显式claims，尤其是位置输出与远端实体。

## 几何继承

入射方向仍由TriggerGeometry携带。`clear(context)` 或显式写零会清除当前上下文的方向（包括旧Iron键）；null写入表示保留。零向量的scoped调用明确遮蔽外层方向，避免旧入射方向因线程作用域而重新出现。形态转换是否保留之前的入射方向由调用方选择，新命中实体的朝向仍优先于入射方向。

共享测试覆盖provider-only友军、独立伤害归属、实体／位置采样与失去授权、实际actor占用、嵌套冲突、共享引导、逐会话清理及几何重置。原生动画、伤害、费用节拍和客户端绘制均留在附属。
