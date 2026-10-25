# ANS 载体接入 0.1.29（2026-10-25）

- 用户目标：Ars ’n Spells 的卷轴、物品铭刻与 Iron 法术轮能完整进入 Conflux 的统一准备和冷却流程。
- 工作树：`parallel-workspaces/conflux-ans-carriers-20261025/Ars-Conflux`，分支 `feature/ans-carriers-20261025`。
- Core 起点：`3183fd54010affc5e37b9eb670519a83c146d0693`。本任务明确修改 Core，不修改任何附属或上游 JAR。
- 版本：Conflux 0.1.29；ANS 3.4.0；Iron 1.21.1-3.16.3 / 1.20.1-3.16.3；NeoForge 21.1.250 / Forge 47.4.0；JDK 21 / 17。

## 实现

- `AnsActiveCasts` 维护可选入口重入边界，仍由 `ActiveCasts` 拥有等待会话。反射方法延迟初始化，不引入 ANS / Iron 硬依赖。
- 通用载体准备前先调用 ANS 原有描述符/预算验证，再读取完整编排。延迟后重新执行原始 ANS 方法，不保存其 ThreadLocal 付款状态。
- 原生代理在 `attemptInitiateCast` 之前准备，实际 `onCast` 内同步进入 Core。不能递归调用外层 `castSpell`，因为 ANS 在这里已经持有独占付款 frame。
- NeoForge 的组件入口与 Forge 的 NBT + Result 入口分别挂钩。Mixin 插件仅对匹配签名启用适配。现有 ANS 黑名单、预算、学派、定价、原生槽冷却和制作途径保留。
- 具体行为与制作边界见 `ars-n-spells.md`。

## 验证

1. 根目录 `:runGameTestServer :jar :sourcesJar -PwithArsNSpells=true -PgameTestNamespaces=ars_conflux_ans --offline --console=plain --no-daemon`：6 项 required GameTest 通过，`build-ans-gametest.log`。
2. 根目录 `:runGameTestServer -PgameTestRunDir=run/ans-absent -PgameTestNamespaces=ars_conflux_billing --offline --console=plain --no-daemon`：无 ANS / Iron 的启动与原生计费回归 1 项通过，`build-ans-absent.log`。
3. `forge/` 下 `:runGameTestServer :jarJar :sourcesJar -PwithArsNSpells=true -PgameTestNamespaces=ars_conflux_ans --offline --console=plain --no-daemon`：6 项 required GameTest 通过，`forge/build-ans-forge.log`。发行包已重混淆。
4. 两端发行/源码包检查：含 MIT，无上游类/素材、无测试代码；Forge 只嵌入既有的 MixinExtras。依赖 SHA 存在忽略的 `build/ans-test-runtime.json`，产物记录在 `build/ans-distribution-check.json`。

测试使用真实 ANS 导出、铭刻数据、服务器使用入口、绑定器和原生 Iron 付款生命周期；时长/提供者冷却使用独立的 Core fixture。未启动完整图形客户端、未逐一人工试用所有附属魔符，未做 Forge 无 ANS 的单独启动测试。不存在客户端外观已验证的结论。

## 产物

- `build/libs/ars-conflux-0.1.29-mc1.21.1-neoforge.jar`：`0379d4930785cbf1b32f5375f90fecc4daae89bb6bd70c05c8fe97e6d541c63f`
- `build/libs/ars-conflux-0.1.29-sources.jar`：`e15f25a17e15055b318c48667cb68f664e4664cbdf0a6c3b9d7f5f8523f2aca9`
- `forge/build/libs/ars-conflux-0.1.29-mc1.20.1-forge.jar`：`c38444ccd99daaadc506ca401e77e0cd46875be9b4574e6fffdbd238e330b635`
- `forge/build/libs/ars-conflux-0.1.29-sources.jar`：`47191549ce70432d669d377b1caa77202e1aeafaaa0a159e9303355cddb5c89d`

共享实例部署由主协调者执行；查看对应 deployment 记录。没有推送或上传 CurseForge。
