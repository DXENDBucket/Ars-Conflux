# 原生弹体的后续触发（0.1.24）

`ImpactTriggers.resolve(hit, policy, nativeImpact)` 始终执行原生完整碰撞回调，再决定是否续接。提供者继续负责目标资格、原生激活成功、原生特效/移除、费用和生命周期；此 API 不改伤害数值，不额外发 Ars 伤害事件，不决定是否收费。

- `ENTITY_DAMAGE`：命中实体必须在本次同步原生回调中对该实体的 hurt 返回成功；命中方块允许续接。爆炸伤害到其他目标不能代替选中实体的成功。
- `ACTIVATION`：提供者已确认的区域、照明、纯状态等非即时伤害行为，保留原生激活时续接，不等待未来 tick 的伤害。

观察通过 Core 的 LivingEntity / Entity hurt 包装及 `NativeSpellDamage.hurt`，第三方 Entity override 或自定义伤害入口可用 `ImpactTriggers.damage(target, original)` 接入；返回值原样传回。Goety 已补充原生调用点观察。嵌套、异常恢复外层；伤害监听器中重入的伤害不能为被拒绝的外层调用提供成功凭据。观察关闭后才执行后续编排，其造成的伤害不会反过来批准前面的命中。

不会通过比较 HP 模拟成功；护盾、吸收、真实来源和原生 invulnerableTime 均保留。若原生函数通过 setHealth 或另一个自定义入口伤害，提供者必须接入自己的 authoritative 成功返回值；不自动猜测。纯原版 Ars 的触发规则和没有显式 observation 的攻击不变。

本次明确的非即时伤害入口：Goety 照明弹、冰风暴、束缚射击、状态菌团；灾变原生元素长枪和雷电场的区域激活。海浪原生成功伤害回调、Iron 直接伤害弹体的既有成功钩子保留。
