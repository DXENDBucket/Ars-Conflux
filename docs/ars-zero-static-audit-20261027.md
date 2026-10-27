# Ars Zero 2.0.2 静态接入审查

日期：2026-10-27。目标为 Minecraft 1.21.1 / NeoForge。

后续实施：0.1.33 已在独立 Core 工作树实现可选运行时接线与上述重点修正，见 [Ars Zero 适配范围与验证边界](ars-zero-compatibility.md)。以下保留审查发生时的记录，不将静态判断改写为运行验证。

本次只读取发布 JAR、反编译相关类并核对字节码及现有 Conflux 实现。没有启动 Minecraft、GameTest 或 Gradle，没有安装、替换模组或修改功能代码。传奇怪物 dev.11 的运行验证继续按用户要求暂缓。

## 结论

可以继续做接入准备，但不建议直接视为当前整合包中已可完整使用的附属。优先处理方块搬运的数据保护、魔源重复产出，以及 Zero 主动施法和光束收费与 Conflux 的接线。Temporal Context 的实际语义也比介绍中“上一阶段输出”更窄。

这不是一次启动验证，不能据此保证没有其他类加载、渲染或运行问题。以下区分“发布包代码已确认”和“需要运行验证的影响”。

## 固定基线与证据

- Ars Zero：`ars_zero-1.21.1-2.0.2.jar`，6,774,561 字节。
- [CurseForge 文件 8703997](https://www.curseforge.com/minecraft/mc-mods/ars-zero/files/8703997)，发布日期 2026-08-22。
- SHA-256：`4682b08eb150672666d82370baa8c64a9e543989b5af6f5c164834e02fcf741c`。
- Conflux 实现基线：`b920838a3bf28cac4c09c9dd150c715efff0d2b2`，0.1.32。
- 对照的万法归一环境：Ars Nouveau 5.13.3、Ars Elemental 0.7.10.4、Curios 9.5.1、TerraBlender 4.1.0.8、JEI 19.54.0.429。
- 该实例的 `ars_trigger_billing.enabled = true`。本次没有把当前工作目录“万途归零”当作部署目标。
- 本地证据目录：`C:/Users/57991/Documents/MinecraftDev/audits/ars-zero-2.0.2-20261027/`。包含原包、SHA 清单、提取的元数据、选定类的 CFR 输出、关键类的 `javap -c -p` 输出。
- 工具：CFR 0.152，最大堆 256 MiB；JDK 21 javap，最大堆 128 MiB。只处理类文件，没有执行模组代码。

实际发布包是本次结论的依据；不能用 GitHub 上其他提交的源码替代它。

## 需要优先处理

### A1：方块搬运预处理可能无法保存完整方块实体数据

**发布包代码已确认，具体损失表现尚未运行复现。**

`ArsZeroResolverEvents.onEffectResolving` 在 BEGIN 阶段判断后续含 Temporal Context 和 Anchor 时，会将选中的方块保存到 `Map<BlockPos, BlockState>`，然后先把原位设置为空气。这里的检查包括领地权限和一份不可移动方块判断，但没有统一排除方块实体。

之后 `onEffectResolved` 才调用 `BlockGroupEntity.addBlocksWithStates`。该方法尝试从原位置读取方块实体并保存 NBT；原位置此时已可能是空气。失败回滚 `restoreCapturedBlocks` 也只恢复 BlockState。

因此不能依靠后面的 NBT 保存代码保证机器、容器和存液设备的数据安全。某些方块在移除时也可能产生掉落；本次没有把“存在数据保护缺口”写成“已复现所有容器吞物品”。

建议先让所有搬运入口共用同一资格判断，暂时排除方块实体；若后续需要支持它们，应在改变世界前完整保存组件/NBT，且成功、回滚和卸载都使用相同快照。

证据：`ArsZeroResolverEvents.java:135`、`:143`、`:249`、`:427`；`BlockGroupEntity.java:203`。

### A2：不可搬运标签未被统一尊重

**发布包代码已确认，与公开报告一致。**

`BlockImmutabilityUtil.isBlockImmutable(Block)` 只硬编码基岩、屏障、三种命令方块。没有读取 `c:relocation_not_supported`。虽然 Select 的部分路径另有 `isPistonPushable` 检查，A1 的事件预处理路径没有使用相同限制。

应覆盖实际的方块捕获入口，不能只改 Anchor 的目标落点检查，也不能只修 Select 自身的方法。

[公开报告 #121](https://github.com/zeroregard/Ars-Zero/issues/121)。证据：`BlockImmutabilityUtil.java:24`、`ArsZeroResolverEvents.java:140`。辅助核对：`bytecode/BlockImmutabilityUtil.txt`。

### A3：Convergence 的初始魔源产出重复计算整条编排

**发布包代码已确认。**

每次 Convergence 作用于魔源罐，`ChargerHelper.handleBlockCharger` 都创建新的充能实体，并把 `getConvergenceCost` 作为初始产出。后者遍历整条编排，将所有 Convergence 及其增强费用累加。每个充能实体的首次充能又直接把这个总量加入罐中。

所以同一编排有 N 个 Convergence 时，初始产出可按 N² 增长，实际受罐容量和其他执行条件限制。例如每个基础费用 200，五个实例各使用 1000 的初始产出，而不是各自 200。

同一实现没有把这个初始产出限制到炮塔实际支付的折后魔源。公开的减耗炮塔循环报告与该路径相符。玩家把 Mana 转为 Source 本身是此能力的用途；需要修的是重复归属和炮塔的资源循环，不能把所有转换一并删除。

[重复产出 #124](https://github.com/zeroregard/Ars-Zero/issues/124)、[减耗炮塔 #123](https://github.com/zeroregard/Ars-Zero/issues/123)。证据：`ChargerHelper.java:72`、`:91`；`SourceJarChargerEntity.java:112`。费用累加已另用 javap 核对。

### A4：Zero 法杖和头环没有接入 Conflux 的主动施法事务

**调用链已确认；没有实际测试界面、冷却和混合编排。**

`AbstractMultiPhaseCastDevice.checkManaAndCast` 直接创建原生 SpellResolver，包装为 WrappedSpellResolver，然后调用 `canCast` 和 `onCast/onCastOnBlock/onCastOnEntity`。它不经过 AbstractCaster.castSpell，也不调用 Conflux 的 ActiveCasts。头环使用同一设备分阶段执行入口。

Conflux 0.1.32 的主动吟唱入口主要包装 AbstractCaster / ScryCasterData；解析器层的 `ActiveCooldownDispatchMixin` 只有在存在匹配的 `ActiveCasts.CURRENT` 事务时才检查、记录主动释放。因此 Zero 这条入口不会自动获得统一吟唱、主动冷却检查和释放提交。不能简单宣称“所有冷却都消失”：部分能力仍有自己的持续执行或原生冷却规则。

需要在 Core 做 Zero 的可选入口适配，不在 Iron、Goety 等附属各写一份。开始/持续/结束的事务边界要先明确，不能把每个 TICK 都当成一次新的按键施法来反复吟唱，也不能让开始阶段提交的冷却阻断同一次施法的结束阶段。

证据：`AbstractMultiPhaseCastDevice.java:604`；Core 的 `ActiveCasterChantMixin`、`ActiveCooldownDispatchMixin`、`ActiveCasts.allowDispatch/dispatched`。原包 checkManaAndCast 结构另保存于字节码输出，避免依赖 CFR 未能完整结构化的代码。

### A5：Zero 光束与触发式收费对同一个后续效果存在重叠收费

**双方收费路径已确认；实际最终费用受配置、减耗和能力影响。**

`EffectBeamEntity.setResolver` 会保存后续编排的基础费用。每次命中前，光束按这个费用乘 `beamResolverManaCostMultiplier` 扣 Mana，默认系数 0.1；扣费成功后才执行子解析器。

原版 Ars 后续效果随后仍经过 Conflux `VanillaTriggerBilling.resolve`，当前整合包打开了该功能。双方没有共享“该效果已付款”的契约，所以例如光束后接原版伤害效果，会同时经过 Zero 的 10% 收费与 Core 的效果触发收费。Zero 的报价还直接使用 Spell.getCost，不是 Conflux 附属实际触发资源的统一报价。

接入时需要区分 Zero 光束自身的费用和后续效果费用，明确这 10% 是载体附加费还是后续效果的替代费用，再避免对同一份效果成本重复归属。不能直接禁用全部原生扣费，也不能让 Iron/Goety 等效果因此变成免费。

证据：`EffectBeam.java:122`；`EffectBeamEntity.java:316`、`:418`；`IManaDrainable.consumeManaAndAccumulate`；Core 的 `VanillaTriggerBilling.resolve`。

## 功能和语义问题

### B1：Temporal Context 实际固定读取 BEGIN

**发布包代码已确认。**

`ArsZeroResolverEvents` 的效果前后处理都提前排除非 BEGIN 阶段，后面写入 tickResults/endResults 的分支因而不可达。`TemporalContextForm.resolveFromStoredContext` 又只读取 beginResults。

这不代表 TICK/END 编排完全不执行；它限制的是捕获结果与跨阶段选点。固定抓住最初目标的用途仍有意义，而“每次以上一阶段的新结果继续推进”的用途没有按介绍工作。

不宜只删两处提前返回：还需要确定固定起点与上一阶段快照的语义、每轮替换还是积累结果，以及原来的 Anchor 是否继续锚定初始对象。报告中提出的补丁方向不等于可以直接照抄的完整修复。

[公开报告 #125](https://github.com/zeroregard/Ars-Zero/issues/125)。证据：`ArsZeroResolverEvents.java:111`、`:165`、`:281`；`TemporalContextForm.java:173`。

### B2：Select 自身的方块选择分支把高度条件写反

**反编译和字节码均确认，是本次额外发现。**

`SelectEffect.onResolveBlock` 遍历 AOE 位置时，只在 `isOutsideBuildHeight` 为真时继续后续检查；合法高度内的位置反而直接跳过。后续 `canBlockBeDestroyed` 又拒绝越界位置，因此该分支无法正常累积候选。

具体 Anchor 组合可能通过 A1 的另一条事件路径创建方块组，不能据此说“所有 Select 和重力枪必定无效”。但 Select 自身的高度条件显然与后续资格检查相互矛盾。

证据：`SelectEffect.java:120`；`bytecode/SelectEffect.txt` 中 `onResolveBlock` 的字节码偏移 157–163：调用高度检查，false 时直接跳回循环，true 才继续。

### B3：光束有独立的无敌帧和平衡规则

**发布包代码已确认，是机制差异，不能仅因旧 issue 已关闭就视作修复。**

`EffectBeamEntity.applyBeamDamage` 会暂时将目标 invulnerableTime 设为 0，执行自身伤害后恢复。默认每 tick 自带 0.5 伤害，每份强化额外 0.5；这是光束本体的伤害，不代表后续所有效果都同样绕过无敌帧。

它每 tick 做最长约 256 格射线和大范围实体候选查询，默认生命周期 5 tick；持续续命和分裂时工作量会累加。这是后续性能验证的重点，本次没有测量 TPS、帧率或内存增长，不能给出“很卡”或“没有性能影响”的结论。

证据：`EffectBeam.java:123`、`:224`；`EffectBeamEntity.java:388`、`:447`。

## 暂不判定为当前版本的确定缺陷

- **1.20.1 永久失重报告**：本次 1.21.1 包已经有序列化 GravitySuppressionAttachment、到期恢复、状态移除恢复和离开世界恢复。没有确认同一故障仍存在；也没有做保存退出/换维度实测来宣称完全修好。[报告 #122](https://github.com/zeroregard/Ars-Zero/issues/122)
- **光束闪烁**：渲染代码有刻意逐 tick 改变的微小随机偏移，视锥判定只检查起终点附近的小包围盒，而不是整段光束。这些是检查方向，尚不能认定是公开报告的唯一根因。[报告 #120](https://github.com/zeroregard/Ars-Zero/issues/120)
- **缺前置导致启动失败**：此发布包明确硬依赖 Ars Elemental 和 TerraBlender；万法归一已有它们以及 Ars/Curios，对声明的依赖范围未发现缺项。是否能完整启动仍未验证。
- **JEI 入口**：当前 JEI 发布包仍保留 Zero 重定向的 RecipeLayout.drawRecipe → IRecipeCategory.draw 调用；Zero 的插件也会在没有 JEI 类时跳过这两个 JEI mixin。这只能排除所检查的明显目标缺失，不等于 JEI 所有页面已验证。
- **解析器子类**：WrappedSpellResolver.resume 调用 super.resume，getNewResolver 也先调用 super，因此已有的基础效果触发、派生上下文和大部分标准 Ars 弹射物接线并非全部绕开。不要为了主动入口的问题复制整套解析器。
- **世界生成**：此包确实读取 BLIGHT_FOREST_WEIGHT，而非旧报告中的硬编码权重。它还带群系、结构和生物，加入整合包时需要单独决定是否启用这些内容。
- **许可证标注**：发布包元数据写 MIT，CurseForge 页面写 GPLv3。本次没有复制实现进 Core；若后续需要直接复制上游代码，先确认实际对应版本的授权，不能只据其中一处标签决定。

## 建议的后续范围

1. 先处理原生的方块捕获安全和 Convergence 产出归属，同时修 Select 高度条件。Temporal Context 先确定语义后再改。
2. 在 Core 增加可选 Zero 入口适配，复用统一上下文、主动施法和收费契约；保持不安装 Iron/Goety 时也能独立使用。
3. 运行条件允许后只补针对性验证：三阶段取消/换手/空蓝、一个有吟唱冷却的附属效果、光束后接原版收费效果、减耗炮塔、容器搬运资格和保存退出后的重力恢复。客户端另看光束/法阵，不为此次审查提前启动游戏。

本次交付为审查文档和本地证据，没有实施上述修复，也没有把模组加入任何实例。
