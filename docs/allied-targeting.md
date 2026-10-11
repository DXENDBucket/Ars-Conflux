# 友军 API 与「周围友军」

## 中立边界

`dev.arsconflux.api.targeting.Allies.areAllies(Entity, Entity)` 查询关系，不创建队伍，不修改生物 AI，也不更改攻击伤害过滤。Core 不导入 Iron 或 Goety 类。

基础关系为：自身、任一方向的原生 `isAlliedTo`、原版 `OwnableEntity` 所有者及共享所有者。所有者 UUID 即使暂时找不到实体也有效；所有权链最多读取八层、按 UUID 去重，避免循环。宠物主人的原生队伍关系也参与判定。中立、被动、同物种不自动等于友军。

附属在 common setup 注册额外的正向关系：

```java
AutoCloseable registration = Allies.register(
    ResourceLocation.tryParse("my_addon:allies"),
    (first, second) -> MyRelations.areAllies(first, second));
```

重复 ID 会报错。查询是对称的，会检查提供者的两个方向；提供者不能再调用 `Allies.areAllies`，也不能以 `false` 否决原有友军关系。关闭注册句柄只移除该注册。注册与查询在游戏线程使用，不做实体缓存。

## Goety 核对

检查了本地实际 NeoForge Goety JAR：`neutral.Owned` 实现 `OwnableEntity` 并重写 `isAlliedTo`；`ally.Summoned` 继承 Owned。主人、同主仆从、原生的所有权关系因此可直接识别，不需要安装 Goety & Nouveau，也不需要 Core 硬依赖 Goety。

Goety 的 `MobUtil.areAllies` 还包含其他规则，例如玩家间的私人盟友信息；不能把它与原版队伍完全等同。此版本只保证原生实体友军与所有者接口覆盖的关系。附属可通过上述 API 补充专有名单，不在 Core 写 Goety 专用判定。

## 形态与上下文

- ID：`ars_conflux:glyph_nearby_allies`，形态，默认三级，默认 30 Mana。
- 围绕实际施放实体的位置选取友军；关系依据原始施法者判定。
- 基础半径 8 格，扩散每份 +3，最多 32；球形距离过滤，查询已有加载实体，不强制加载额外区块。没有额外视线条件。
- 默认排除原始施法者和实际施放实体，以及死亡、移除和旁观实体。
- 按距离、实体 ID 稳定排序，每个目标克隆独立解析器。前一个目标不会耗尽下一个目标的效果游标；每个目标内效果仍按原 Ars 顺序运行。
- 选中友军只改变命中结果。克隆保留原始施法者、伤害所有者、来源链与资源账户。目标不自动成为付款者或执行者。
- 每个目标显示对应版本 Ars 的自身施法粒子：Neo 使用原生 emitter，Forge 使用原生 TIMED_HELIX 数据包。
- 未改变支付策略。Ars 默认预付效果费用一次；若用户开启 Core 原生效果触发计费，原生效果也逐目标支付。附属触发费用独立逐目标结算。

## 启动配置与配方

发行默认 `enable_nearby_allies = false`；Neo 的 `enable_elemental_phalanx` 也改为 false。关闭时不注册新友军魔符物品、不加载其配方，API 始终可用。

NeoForge 用 STARTUP 配置。Forge 1.20.1 没有 STARTUP 类型，因此在注册前只读同名文件，随后由 Forge COMMON 配置正常校验和写入。无文件时默认关闭；必须在重启前修改 `config/ars_conflux-startup.toml`，服务端与客户端保持相同值，不支持热切换注册。

友军魔符使用真正的 Ars 抄写台配方：召唤精华、操纵精华、魔源宝石各一，经验 160。配方通过各加载器的条件系统门控，不提供 JEI 信息页代替配方。

「万法归一」显式打开两个开关；不会修改其他整合包的配置。Forge 不包含上游不存在的卡利亚圆阵。

## 验证范围

新定向测试检查开关对应的魔符／物品／配方注册、原版队伍／所有者／共享所有者／外部提供者、非友军排除、扩散选择和两种后续效果的独立续接，并检查原施法者／伤害归属／资源账户没有改变。

客户端图标、粒子和多人画面需要人工确认。常见 Goety 所有权行为由实际 JAR 接口／字节码核对；没有把 Goety 专有名单或所有仆从类型都标成实测。
