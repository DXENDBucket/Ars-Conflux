# 汇流法术强度与流派法术强度（0.1.42）

## 百分比属性

| 属性 | 默认值 | 作用 |
| --- | --- | --- |
| `ars_conflux:confluence_spell_power` | 0 | 汇流法术强度；不要求声明流派 |
| `ars_conflux:school_power/<namespace>/<path>` | 0 | 对应流派的法术强度；只按该流派权重生效 |

`0.2` 代表 +20%，`-0.2` 代表 -20%。基础值和修饰符均使用百分比的小数表达；给装备添加 +20% 时使用 ADD_VALUE（Forge ADDITION）和数值 `0.2`。单项属性范围为 -1024 到 1024，合计后才限制最终倍率非负。属性注册到玩家和已有属性供应器的其他生物，并同步客户端。原生属性命令、装备 modifier 和其他模组可使用它们；本版不额外赠送成长或装备。

汇流与流派加成同乘区加算：

```text
法强倍率 = max(0, 1 + 汇流法强 + Σ(流派权重 × 对应流派法强))
```

例如汇流 +20%、辉耀 +50%，法术含 70% 辉耀和 30% 凋亡（凋亡加成为 0），得到 ×1.55。未声明流派时为 ×1.2。负数可以削弱，总倍率最低为 0，不会变成治疗。

目标克制规则仍是独立的加权倍率，只有 `[schools].damage_rules = true` 才启用；这个开关不控制法强属性。原有 Ars 平加伤害、Iron/Goety 原生装备和法强保留原先的计算，汇流倍率在其后的伤害边界计算一次，再进入无敌帧、护甲、抗性等原生结算。

这一版的法强作用于法术**伤害**，不自动增加治疗、持续时间、药效等级或召唤物本身的属性。普通近战、环境伤害和普通召唤生物的自主近战不因为持有者的汇流法强而被全局修改。原生反馈自伤费用保留原值。

## 自定义流派的属性注册

Core 不预设流派，默认只有汇流属性。由于 Minecraft 属性是启动注册表，不能在 `/reload` 中新增属性。需要流派属性时，在客户端和服务器的 `config/ars_conflux-startup.toml` 中声明相同列表并重启：

```toml
[schools]
power_attributes = ["example:radiance", "example:decay"]
```

这会注册：

```text
ars_conflux:school_power/example/radiance
ars_conflux:school_power/example/decay
```

列表不会定义流派或自动给魔符归类。继续用 [流派数据包](schools.md) 提供名称和权重；修改归属、权重、目标克制仍支持 `/reload`。未注册或不存在的流派属性按 0 处理，其他分量继续正常计算。

给自己设置加成（需要权限）：

```mcfunction
/attribute @s ars_conflux:confluence_spell_power base set 0.2
/attribute @s ars_conflux:school_power/example/radiance base set 0.5
```

流派属性有可读的通用名称回退。资源包可以进一步翻译为自定义名称，例如：

```json
{
  "attribute.ars_conflux.school_power.example:radiance": "辉耀法术强度",
  "attribute.ars_conflux.school_power.example:decay": "凋亡法术强度"
}
```

NeoForge 用属性自身的百分比 tooltip；Forge 的普通装备说明仅对这些属性将加算数值显示为百分比。其他模组自行绘制的属性面板可能有自己的数值格式。

## 统一伤害接入

- 普通 Ars 效果分发、Conflux `CastContexts` 和 `ExecutionSession` 共用作用域。形态替换使用替换魔符自身的流派，后续效果重新选择自己的流派。
- 公共 `NativeSpellDamage` 路径保留一次 Ars 平加值与 Pre/Post 事件；汇流与流派倍率在 Pre 阶段应用，原生 hurt 不再叠加。
- Iron/Goety 这类保留原生伤害路径的调用，在对应作用域内于最终 native hurt 应用一次；其内部若再发 Ars Pre，Core 会将本次倍率留给最终伤害，避免两次叠加。
- 非生物弹体／区域在受管理的施法作用域中加入世界时捕获原施法者与不可变流派快照。其 native tick、延迟伤害和子载荷继承这份归属，不改变运动、模型、施法者／付款者或伤害来源。
- `ExecutionSession` 同样为后续回调保留流派快照。各次真正命中读取原施法者当前的法强属性；不使用代为执行的友军的法强。
- Iron 的持久冲锋和付费区域／发射器绑定保存流派，原生自身施法 lease 保留流派；Goety 的 Bolting 延迟任务显式恢复捕获的归属。
- 独立调度、既不使用这些入口也没有受管载荷的能力仍需要提供者显式接入。不要把任意模组的普通攻击全局视为法术。

供附属使用的 Core API：

```java
var id = SpellPower.attributeId(mySchool);
var factor = SpellPower.multiplier(caster, schools);
var profile = new SpellPower.Profile(caster, schools);
SpellPower.bind(payload, profile); // 不需要叠加 Ars 平加值的原生载荷
SpellPower.scoped(profile, nativeCallback);
SpellPower.tick(payload, nativeTick);
```

已有 `NativeSpellDamage.bind/hurt` 调用不需要迁移；旧签名保持可用。`CastContext` 的记录字段和构造签名保持不变。附属不要先自行乘一次这个倍率再提交公共入口。

载荷资料保持运行时生命周期：世界卸载／服务器停止时清理，不强制加载区块；通用绑定不是实体存档格式。提供者自己的持久行为应保存所需归属并恢复。
