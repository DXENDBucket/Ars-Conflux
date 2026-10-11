# Ars Controle / Ars Additions

Conflux 0.1.15 提供可选修正。不复制附属的内容、资源或配方，不要求安装 Iron、Goety 或 BOMD。

| 平台 | 核对的发行版 | 本次处理 |
| --- | --- | --- |
| NeoForge 1.21.1 | Controle 1.6.16 / Additions 21.3.0 | 跨维度棱镜、二元随机过滤、反击 |
| Forge 1.20.1 | Controle 1.1.1 / Additions 1.6.7 | 跨维度棱镜、反击 |

Forge 的 Controle 没有随机过滤和精确延迟，不补移植。Additions 的标记／回溯和 NeoForge Controle 的精确延迟继续沿用上游解析器与调度，当前没有为它们新增语义。

## 传送棱镜

原生棱镜跨维度会复制并生成新的 Ars 弹射物，再移除旧弹射物。这不适用于借用 Ars 轨迹、但以另一模组原生实体呈现的替换弹射物：提供者还持有旧轨迹和原生实体。

可选适配只在 `ArsTrajectory` 已管理的载体上接管 `changeSpellLevel`。保留载体实例及其原生运动状态，由棱镜继续决定位置、命中、费用和目标加载。原生呈现实例在目标位置确定后迁移；直接命中实体时，在提供者载荷执行前迁移。不会把不可见的 Ars 载体另外生成到世界里。

`ExecutionSession.world()` 是执行世界，`originWorld()` 是原始施法世界。只有显式轨迹迁移才能改变执行世界；施法者与原来的活体执行者仍须存活且留在原始世界。时限沿用原始世界时钟，不因迁移重置。卸载任一相关世界会结束执行。

会话自动记录 `driver.start` 内成功生成的 `Projectile`；`ArsTrajectory` 另外登记自己的呈现实例，即使它不是 `Projectile`。批量原生弹射物随主轨迹迁移，保留实例、UUID、会话字段和归属。不得自动记录命中时产生的独立子弹射物或召唤物，以免混淆生命周期。提供者在启动之后生成会话自有弹射物时，可显式调用 `trackProjectile(entity)`。

迁移使用服务端原生实体移除／加入的跟踪流程。目标拒绝生成时，中止载荷并清理整批自有弹射物；不退回免费 Ars 弹射物。提供者继续负责通常的关闭与原生行为。

已绑定的 Ars 上下文附件带有不可变的施法链标识。冻结接续仍传入原始世界时，Core 仅在当前会话、同一施法链且已经迁移的条件下修正解析世界；同一施法者发起的独立法术不会被迁移。自定义解析器如果完全覆写 `onResolveEffect` 而不调用基类，应显式使用 `ExecutionSessions.current().world()`，并保留自身解析行为。

普通 Ars 弹射物仍使用 Controle 原有的复制迁移。1.20.1 目标必须已经加载；1.21.1 的临时区块加载和两版的魔源费用均由 Controle 决定。本次不扩展跨维度召唤物、传送门或自动区块加载规则。

## 随机过滤（仅 1.21.1）

修正二元过滤器第二个随机条件读取第一个条件增强的问题。两次出现的随机魔符本身是同一个注册实例，因此两边各保存一份概率快照，避免第二次配置覆盖第一次。概率仍调用 Controle 原生公式；OR 的短路和 XOR／XNOR 的运算沿用上游。

## 反击

Minecraft 在部分受伤回调结束后才更新上一次攻击者，导致反应式法术第一次受击没有目标，或随后指向上一位攻击者。

公共 `RetaliationTargets` 在同步 `hurt` 调用内保存本次伤害来源。Additions 的反击在这个范围内读取实际攻击者；无生物来源的伤害不会借用旧攻击者。主动使用反击、没有当前伤害回调时，保留 Additions 自己的伤害记录规则。嵌套伤害和异常退出会恢复上层状态，不保存永久攻击者表。

## 验证

将上述官方发行包放入忽略的 `libs/controle-additions/`；Forge 文件名为 `ars-controle-controle1201.jar`、`ars-additions-additions1201.jar`。

```powershell
./gradlew runGameTestServer -PwithControleAdditions=true -PgameTestNamespaces=ars_conflux_optional
cd forge
./gradlew runGameTestServer -PwithControleAdditions=true -PgameTestNamespaces=ars_conflux_optional
```

测试通过真实棱镜与反击入口，验证同实例迁移、原生载体状态、整批呈现、旧世界闭包、施法归属、一次扣款、目标拒绝生成、施法者失效及卸载清理。NeoForge 另检查两个随机条件分别为 75%／25%，且不会被后续配置覆盖。移除该 Gradle 属性可检查不安装附属的启动与公共路径；这时附属专用用例会跳过。

最终批次：NeoForge 安装附属时 5 项专用测试加 4 项现有核心测试通过；Forge 安装附属时 4 项专用测试通过，现有计费批次也通过。不安装附属时 NeoForge 的 5 项、Forge 的 4 项公共路径／跳过用例通过，确认可选适配不成为启动依赖。

棱镜测试使用已加载目标区块，并暂设跨维度费用为零，仍走真实 `onHit` 和原生普通弹射物迁移；不是对附属收费或自动加载进行完整测试。实体跟踪用例使用始终可跟踪的呈现夹具，避免远程测试世界异步区块可见性影响断言。这些是服务端行为测试；客户端跨维度呈现仍需人工观察。

Forge Controle 1.1.1 的旧 Patchouli 手册定义在当前 Patchouli 下会被跳过（`use_resource_pack=false`）；这是上游资源问题，本次不改手册。
