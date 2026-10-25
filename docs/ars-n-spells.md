# Ars ’n Spells 载体联动（0.1.29）

支持 ANS 3.4.0 的三条现有路径：Spell Loom 导出卷轴、Spell Transcription 铭刻物品（包括武器）、绑定到 Iron 法术书后通过原生法术轮施放。完整 Ars 编排仍由 ANS 保存；其中的 Conflux 附属魔符复用各提供者的既有实现，无需在 Iron、Goety 或其他附属中分别适配 ANS。

## 行为

- 主动使用先经过 `ActiveCasts`。提供者自己的吟唱、冷却与创造模式配置仍适用；吟唱时间继续使用 Core 的 MAXIMUM / SUM 规则，进度和移动影响继续由提供者呈现。
- 等待吟唱不算成功施放，不运行原生效果、不预付 ANS 的 Ars 费用、不启动附属冷却。重复点击保留同一个等待会话。
- 换持物、修改编排、死亡、断线或换维度沿用 Core 的取消规则。铭刻物品释放时还复查持物、菜单、睡眠与载体快照。
- ANS 的选点、学派、跨系统费用倍率、额外资源、法术数据校验及错误消息仍由 ANS 执行。原有原生 Ars 触发计费开关保持独立，不新增一份 Mana 费用。
- Iron 法术轮先完成 Core 准备，再进入 Iron 的原生启动流程。实际代理 `onCast` 发生在之后的原生 tick，届时才开启同步的 Core 执行边界。ANS 的原生付款 frame 保持在原来的调用栈中，不延长、复制或重新进入它。
- 原生代理槽的冷却仍由 ANS / Iron 记录；附属冷却由对应提供者在真实 Ars dispatch 成功后记录。缺资源、取消或未成功 dispatch 都不提交附属冷却。
- 从 Curios 法术书经由手持工具启动时，额外校验原始法术书及其数据未改变。实际付款者仍是施法玩家。

ANS 的通用物品铭刻和“卷轴通过 Iron 铭刻台绑定原生法术书”仍是不同的制作路径。本联动不扩展 ANS 原生法术书绑定器的目标种类，也不改变卷轴是否消耗的上游规则。关闭 ANS 的 Imported Iron Glyphs / Glyph Press 不会关闭这些 Ars 编排载体。

## 可选加载与许可

没有 ANS 或 Iron 都不是缺少硬前置。Mixin 插件只读检查 ANS 入口字节码，匹配 3.4.0 的平台签名后才启用相关挂钩；旧布局不会错误套用新签名。NeoForge 的组件记录和 Forge 的 NBT / structured result 分别接线，公共准备/完成逻辑只有一份。

只调用已安装 ANS / Iron 的现有方法，不包含其实现代码或美术素材；Conflux 继续使用 MIT。接口变动时需要重新核对实际发布 JAR，不能视为未来 ANS 版本的无条件保证。

## 定向验证

使用官方 ANS 3.4.0：

- NeoForge 1.21.1：[CurseForge file 9280543](https://www.curseforge.com/minecraft/mc-mods/ars-n-spells/files/9280543)。
- Forge 1.20.1：[CurseForge file 9280537](https://www.curseforge.com/minecraft/mc-mods/ars-n-spells/files/9280537)。

`withArsNSpells` 只启用测试运行时，不加入发行包。NeoForge 将 ANS、Iron 及 Iron 的可选运行依赖放在忽略的 `libs/ars-n-spells/`；Forge 对应 `forge/libs/ars-n-spells/`，使用 `ars-n-spells-ans1201.jar`、`irons-spells-n-spellbooks-ans1201.jar`、`irons-lib-ans1201.jar`、`playeranimator-ans1201.jar`。基础 Ars 依赖沿用项目布局。

```powershell
# 根目录，JDK 21
./gradlew.bat :runGameTestServer -PwithArsNSpells=true -PgameTestNamespaces=ars_conflux_ans --offline
# forge/，JDK 17
./gradlew.bat :runGameTestServer -PwithArsNSpells=true -PgameTestNamespaces=ars_conflux_ans --offline
# 不带 ANS / Iron 的独立世界，避免上个测试世界的原生维度引用
./gradlew.bat :runGameTestServer -PgameTestRunDir=run/ans-absent -PgameTestNamespaces=ars_conflux_billing --offline
```

测试调用真实 ANS 导出器、铭刻数据、服务器使用入口、绑定器和原生 Iron `castSpell` 生命周期。一个隔离的 Core 提供者负责 5 tick 准备与冷却，以检验跨提供者边界，不依赖某个附属的数值。测试代码及运行依赖不进入发行包。

客户端的进度条、移动手感和完整整合包尚未人工验证。服务端测试不等同于这些画面和手感已验证。

2026-10-25 的实际验证：双平台的 6 项 ANS GameTest 已通过，覆盖卷轴等待与重复输入、铭刻武器取消和重试、原生法术轮成功、原生法术轮空蓝失败、普通 Ars 即时施放、独立来源法术书替换取消。另在 NeoForge 执行不带 ANS / Iron 的原生 Ars 触发计费启动回归（1 项通过）。构建和日志记录在本次交接文档中。
