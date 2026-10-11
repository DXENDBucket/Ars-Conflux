# 原生法术伤害 API（0.1.18）

NeoForge 1.21.1 与 Forge 1.20.1 提供同一套 `dev.arsconflux.api.damage.NativeSpellDamage`。
Core 不依赖 Iron、Goety、BOMD 或灾变。原生伤害钩子、基础值、百分比项、增强、伤害类型、攻击次数、粒子和费用仍由附属定义。

## 显式接入

```java
NativeSpellDamage.bind(payload, context); // 临时附件，不保存到存档、不网络同步
NativeSpellDamage.hurt(context, target, nativeSource, nativeAmount, target::hurt);
// 原生同步爆炸或其他无法逐个改写的 LivingEntity.hurt：
NativeSpellDamage.during(context, () -> level.explode(/* 原生参数 */));
```

`hurt` 在服务端读取 `context.caster()` 的 Ars `SPELL_DAMAGE_BONUS`，加到提供者算好的数值上，发布 Ars `SpellDamageEvent.Pre`，尊重取消及修改后的伤害和来源，调用原生伤害函数；只有函数返回成功才发布 `Post`。`executor` 和选中的目标不会自动变成施法者；原生伤害归属来自传入的 DamageSource，Core 不替换它。设备或无生物施法者当前回退原生伤害。无附件、无作用域的原生攻击不产生这些事件。

`bind(entity, null)` 删除附件；子实体必须显式继承附件，或直接保存/传递 CastContext。附件通过弱键保存，并在维度卸载和服务器停止时清理，全部操作在服务器线程进行。客户端附件不承担同步功能。

`during` 只覆盖同步代码段；它不是持续施法会话，也不会隐式传给未来 tick 的任务。嵌套和异常会恢复外层作用域；传入 null 暂时屏蔽外层。Core 的 `LivingEntity.hurt` 包装仅在此显式作用域中工作。跨维度等生命周期仍由提供者和现有 ExecutionSession 管理。

## 防止重复加成

Core 的共享、线程局部重入保护覆盖 Pre、原生 hurt 与 Post。附属直接伤害钩子与 Core 作用域钩子同时经过同一击时只加一次；所有接入者使用同一个保护。监听器中重入的伤害回调走原生路径，不再次加成或递归发事件。新生成的独立攻击应安排独立触发，而不要用监听器重入模拟多个伤害事件。

已经被 Ars 或其他事件桥接处理过的数值不要再次提交。Iron/Goety 自身的法强和 Ars ’n Spells 的 Iron 事件路径不在本次抽取范围内，不能无条件套用此 API。原版 Ars 魔符和 Core 触发计费开关不受影响。

## 数值边界

API 处理提供者提交的整次伤害值，不重算 SpellStats，也不改变原生血量比例、上限或攻击次数。外部 Pre 监听器若乘整次伤害，会同时放大数值中已有的血量比例贡献。火焰、中毒等后续状态伤害不会自动继承附件；治疗也不会被这个伤害 API 放大。费用、吟唱、冷却均不在此接口中结算。

BOMD & Nouveau 和 Cataclysm & Nouveau 首先接入。附属只保留原模组对应的命中钩子及特殊上下文查询，不再各自保存通用伤害状态或给 LivingEntity 安装重复的爆炸钩子。
