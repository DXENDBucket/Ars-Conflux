# Hex-Ars Linker 可选联动

Hex 的 Ars 施法入口属于 Conflux 的公共适配边界。Iron、Goety 或其他魔符提供者只实现 Core 的载体与效果接口，不需要引用 Hex 类、检测 Linker 的解析器，或分别维护 Hex 专用转换代码。

## 通用链路

- 常规施法经由 Ars 的施法入口与公共载体钩子。
- 指定位置的 Linker 操作自行创建 Ars 弹射物，由 Core 的实体加入世界钩子执行 `ProjectileDispatch`。
- 指定实体的操作经过 Ars 的目标解析，效果提供者从 `CastContext` 取得目标与施法归属。

所有提供者共用 `ProjectileForm`、`CarrierRegistry`、`ArsSpellAccess`、`CastContext` 和资源事务。提供者不需要知道解析器是不是 `PatternResolver`；没有提供者的替换形态时，原生 Ars 弹射物保持原样。

继续编排和延迟采用 Ars 上下文克隆，Core 的不可变归属附件随之保留。选择目标不会自动把目标变成施法者、伤害所有者或付款者。

0.1.5 增加公共 `ResolverExtension` / `ResolverContexts`。Core 的可选 Linker 适配记录原生环境，普通子解析器恢复 Linker 定价，碰撞包装保留自己的执行逻辑并传递来源附件，触发报价使用原生来源工厂。回调从该来源读取深度，继续遵守 Linker 自带的递归限制。法术提供者不需要自行检测 `PatternResolver`。

## 资源与行为范围

Hex 的基础媒质费用及 Linker 的额外媒质报价继续由 Hex／Linker 处理。魔符提供者保留自己的报价、资源来源与资格检查；Core 不把 Iron Mana、Goety 灵魂能量或 Hex 媒质强行合成一个池。

默认仍为原生 Ars 起手计费。开启 `ars_trigger_billing.enabled` 后，原生 Ars 效果移到触发时支付，保留 Linker 的 `costRatePatternMana` 倍率。人工形态省略仅在起手生效，不从各次效果反复减去；空蓝的分支停止，创造施法免费。Ars 5 拆开了资格报价与实际扣款，Core 同时适配两者，避免原生 Linker 只缩放报价却扣除完整费用。

`ratioExtraMediaCasting` 的额外媒质报价仍基于完整原生 Ars 术式，移到命中时付款不会使该报价漏掉后续原生效果。媒质实际支付由 Hex／Linker 完成。代施法时，运行 Hex 的玩家支付媒质，被选中执行 Ars 法术的玩家支付自己的 Ars 费用；选择普通攻击目标不会改变付款归属。

Core 的 `SpellResourceQuotes.estimate(context)` 可只读汇总提供者各自的首次触发报价，同一个实际资源池的费用合计判断；未实现报价的提供者保留未知条目。这个接口不改写 Linker 的原生 Mana 查询含义，也没有新增 Hex 查询图案。它未计入持续、散射和重复次数，不能据此保证整个编排都能完成。

吟唱、原生冷却和持续效果仍由各法术提供者声明和处理。这一层不会让整个 Hex 程序自动等待吟唱结束。Linker 自行创建的指定实体／位置操作仍使用其原生触发方式。

## 可选性

Core 在两个加载器的元数据中声明 Linker 为可选模组，没有 Hex／Linker 硬前置。发行包不包含 Hex、Linker、测试提供者或这些模组的资源。转换仍走通用 Ars API；专用来源适配封装在 Core 内部的可选 mixin 和延迟反射桥，只有安装 Linker 后才访问其环境与配置类型。未安装时不加载 Hex 类，公共 API 没有 Hex 类型签名。

Iron 中旧的 Linker 类型检测、可选依赖声明和专用 Forge 测试已归并到 Core 的验证范围。新的提供者只需要依赖 Conflux。

## 定向验证

独立环境只装 Core、Ars、Hex、Linker 与相应前置，不装 Iron、Goety 或其他法术提供者。共同测试用两个互不认识的提供者，分别检查常规施法、指定位置发射、指定实体效果、归属与方向、克隆、单次替换和空资源拒绝。

固定测试版本：

| 平台 | Hex Casting | Hex-Ars Linker |
| --- | --- | --- |
| 1.21.1 NeoForge | `0.12.0-devel-pre-67` | `1.21.1-0.9.5.2.pre67` |
| 1.20.1 Forge | `0.11.4` | `1.20.1-0.9.5.1` |

这些是锁文件中的定向验证版本，不表示所有 Hex 预览版 API 都兼容。2026-10-10 的历史服务端验证使用 pre53；2026-11-04 更新 NeoForge 锁文件至 pre67。各次实际验证结果分开记录，服务端测试不代表客户端外观验证。

2026-10-10：双版本定向验证包含两项 Hex GameTest 与一项原生 Ars 计费回归：

- 两个独立提供者的常规、指定位置、指定实体入口，归属、方向、克隆、单次转换、同池费用合计和空资源拒绝。
- 起手及触发两种 Ars 计费模式、非默认 Mana／媒质倍率、派生解析器、人工形态省略、实际扣款、空蓝和创造免费。
- 共享轨迹碰撞包装后仍保留回调深度和原生递归限制；实际 Ars 命中 → 回调 Hex VM → Ars 目标效果，并检查真实媒质支付。
- 完整 VM 的指定方块与另一玩家代施法，检查施法结果及两名玩家的付款归属。

Forge 的代施法属于原生逐世界图案且需要启迪，测试使用该世界生成的图案并给测试玩家授予启迪；Core 不取消这些原生要求。未安装 Hex／Linker 的启动和原生计费路径也单独验证。没有安装 Iron 或 Goety；结果验证公共链路，不表示 Goety 的具体法术已经接入。

准备 Core 原有 Ars 开发依赖后，从 Core 根目录运行：

```powershell
python tools/fetch_hex_test_dependencies.py
./gradlew.bat :runGameTestServer -PwithHexLinker=true '-PgameTestNamespaces=ars_conflux_hex,ars_conflux_billing'
cd forge
./gradlew.bat :runGameTestServer -PwithHexLinker=true '-PgameTestNamespaces=ars_conflux_hex,ars_conflux_billing'
```

`hex-test-dependencies.lock.json` 记录下载地址与 SHA-256，文件写入忽略的 `libs/hex-link/` 与 `forge/libs/hex-link/`。不传 `withHexLinker` 时这些依赖不会加入运行时。完整 VM 操作、测试玩家与测试提供者只存在于 gameTest 源码，不进入发行包。

上游代码：[Hex-Ars Linker](https://github.com/YukkuriC/HexArsLinker)。

## 主动操作准备与冷却（0.1.22）

常规施法、指定实体效果和指定位置发射都使用 `ActiveCasts`。一次 Hex 操作是一次主动使用；需要准备时延后的是该操作的 Ars 释放，Hex VM 本身继续执行，选点/选实体由原动作字段保留。已经支付的媒质不新增退款机制。原有死亡、换维度、换持物和准备取消规则仍生效。

`[hex_linker].chanting` 与 `cooldowns` 默认均为 true，可独立关闭；保留提供者自己的吟唱/冷却开关。该策略随解析器克隆和会话上下文继承，不更换 Mana 或媒质费用。原编排产生的 `GlyphCallbackCastEnv` 属于被动回调，不重新起手或登记另一轮主动冷却。

可选入口使用普通 Mixin HEAD 注入和短暂的动作身份防重入，不要求 MixinExtras 新版的 `WrapMethod @Coerce` 支持；Forge 0.4.1 最低环境仍可接入。可选类型仅在已加载 Linker 后反射查找，不加入公共 API 签名或发行包。

## Hex 客户端服务加载修复（0.1.51）

NeoForge 的 Hex pre53、pre64 和 pre67 使用 `ServiceLoader.load(service)`，会依赖当前加载线程的上下文类加载器。在模组加载期间，这可能找不到 Hex 自身的客户端平台实现。Conflux 将这一调用改为 `ServiceLoader.load(service, service.getClassLoader())`，使用服务接口所属模组的类加载器。修复不依赖 Linker，也不改变媒质、Mana、图案或施法规则。

这一 Mixin 仅登记在 NeoForge 的客户端列表：发现 Hex 的精确版本属于上述已审查版本、原目标 `find()` 中仍恰有一处单参数加载调用，且没有安装旧 `hex_pre53_client_fix` 时才启用。未知版本或上游改过目标调用时不应用。判断读取加载元数据和 class 字节，不提前初始化 Hex 客户端服务。没有 Hex、专用服务器、Forge 1.20.1 都不加载该修复。

从独立补丁迁移时，请移除 `hex-pre64-client-fix-0.1.1-mc1.21.1-neoforge.jar`（旧版文件名可能为 pre53），使用 Conflux 0.1.51 或更新版。旧 modid 仍为 `hex_pre53_client_fix`；若它还在，Conflux 会避让，避免两个 Redirect 争用同一调用。但 Conflux 不能解除旧补丁自身的精确 pre64 依赖，因此升级 Hex 时必须移除旧 JAR。

NeoForge 最新候选包的来源、API 对照和完整性检查见本地 `inspection-cache/upstream-compat-20261104/`。可运行两项定向测试：

```powershell
./gradlew.bat :test --tests dev.arsconflux.internal.hex.HexClientServicesSupportTest --offline
# 需要有足够内存；只加载隔离菜单，确认真实 Hex 服务实例后自动退出，不打开存档。
./gradlew.bat :runClient -PwithHexLinker=true -PhexClientSmoke=true --offline
```

客户端检查类只在 gameTest 源码集中，不进入发行 JAR。
