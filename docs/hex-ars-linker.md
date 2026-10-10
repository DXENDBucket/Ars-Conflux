# Hex-Ars Linker 可选联动

Hex 的 Ars 施法入口属于 Conflux 的公共适配边界。Iron、Goety 或其他魔符提供者只实现 Core 的载体与效果接口，不需要引用 Hex 类、检测 Linker 的解析器，或分别维护 Hex 专用转换代码。

## 通用链路

- 常规施法经由 Ars 的施法入口与公共载体钩子。
- 指定位置的 Linker 操作自行创建 Ars 弹射物，由 Core 的实体加入世界钩子执行 `ProjectileDispatch`。
- 指定实体的操作经过 Ars 的目标解析，效果提供者从 `CastContext` 取得目标与施法归属。

所有提供者共用 `ProjectileForm`、`CarrierRegistry`、`ArsSpellAccess`、`CastContext` 和资源事务。提供者不需要知道解析器是不是 `PatternResolver`；没有提供者的替换形态时，原生 Ars 弹射物保持原样。

继续编排和延迟采用 Ars 上下文克隆，Core 的不可变归属附件随之保留。选择目标不会自动把目标变成施法者、伤害所有者或付款者。

## 资源与行为范围

Hex 的基础媒质费用及 Linker 的额外媒质报价继续由 Hex／Linker 处理。魔符提供者保留自己的报价、资源来源与资格检查；Core 不把 Iron Mana、Goety 灵魂能量或 Hex 媒质强行合成一个池。

这次独立验证使用 Core 默认的原生 Ars 起手计费。开启 `ars_trigger_billing.enabled` 时，Linker 的媒质报价与自定义解析器倍率仍需单独验证；不要将默认模式的测试结果理解为已经覆盖这项组合。

吟唱、原生冷却和持续效果仍由各法术提供者声明和处理。这一层不会让整个 Hex 程序自动等待吟唱结束。Linker 自行创建的指定实体／位置操作仍使用其原生触发方式。

## 可选性

Core 在两个加载器的元数据中声明 Linker 为可选模组，没有 Hex／Linker 硬前置。发行包不包含 Hex、Linker、测试提供者或这些模组的资源。运行时转换代码通过通用 Ars API 工作，不加载 Hex 类。

Iron 中旧的 Linker 类型检测、可选依赖声明和专用 Forge 测试已归并到 Core 的验证范围。新的提供者只需要依赖 Conflux。

## 定向验证

独立环境只装 Core、Ars、Hex、Linker 与相应前置，不装 Iron、Goety 或其他法术提供者。共同测试用两个互不认识的提供者，分别检查常规施法、指定位置发射、指定实体效果、归属与方向、克隆、单次替换和空资源拒绝。

固定测试版本：

| 平台 | Hex Casting | Hex-Ars Linker |
| --- | --- | --- |
| 1.21.1 NeoForge | `0.12.0-devel-pre-53` | `1.21.1-0.9.5.2.pre53.fix2` |
| 1.20.1 Forge | `0.11.4` | `1.20.1-0.9.5.1` |

这些是测试使用的版本，不表示所有 Hex 预览版 API 都兼容。独立实验工程中的 Hex Pre53 客户端平台服务修复不属于 Core；本次服务端测试也不验证客户端渲染。

2026-10-10：两个平台的真实 Linker 定向 GameTest 均通过，每个平台的一项测试依次验证两个独立提供者的上述入口与拒绝条件。测试没有安装 Iron 或 Goety；它验证公共链路，不表示 Goety 的具体法术已经接入。

准备 Core 原有 Ars 开发依赖后，从 Core 根目录运行：

```powershell
python tools/fetch_hex_test_dependencies.py
./gradlew.bat :runGameTestServer -PwithHexLinker=true -PgameTestNamespaces=ars_conflux_hex
cd forge
./gradlew.bat :runGameTestServer -PwithHexLinker=true -PgameTestNamespaces=ars_conflux_hex
```

`hex-test-dependencies.lock.json` 记录下载地址与 SHA-256，文件写入忽略的 `libs/hex-link/` 与 `forge/libs/hex-link/`。不传 `withHexLinker` 时这些依赖不会加入运行时。测试反射只存在于 gameTest 源码，不进入发行包。

上游代码：[Hex-Ars Linker](https://github.com/YukkuriC/HexArsLinker)。
