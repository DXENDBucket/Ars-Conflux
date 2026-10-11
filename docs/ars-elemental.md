# Ars Elemental

Conflux 0.1.13 提供可选适配。Elemental 不成为必需依赖；发行包不包含其类、素材或 JAR。Iron、Goety、BOMD 继续通过公共 `CarrierRegistry` 和 `ArsTrajectory` 接入，不需要各自加入 Elemental 补丁。

## 再次发射

Elemental 的 `PropagatorArc`、`PropagatorHoming` 与 NEG 的对应实现共用预算钩子。生成前给子解析器声明实际载体，并在独立预算范围内估算可负担数量；退出时通过 `finally` 清理。Elemental 自己负责按减少后的数量生成位置、角度、飞行速度和目标忽略规则。

安装 Elemental 后，NEG 使用 Elemental 的同名魔符实现，因此只适配 NEG 的类不足以覆盖共存环境。这里直接覆盖实际执行的 Elemental 类；1.20.1 的合成配方没有明确的 `castType` 时也保留正确载体身份。

没有提供者载荷时，不减少原有数量。报价和资源池由载荷提供者决定；Core 不假定 Iron 魔力或 Goety 灵魂能量。付款失败的载荷不会退化成免费原生弹射物。弧线再次发射的具体横向位置沿用上游排列。

## 卡利亚圆阵（1.21.1）

**上游限制与 Conflux 开关：** Elemental 0.7.10.4 的 `ArsNouveauRegistry.registerGlyphs()` 仅在 `!FMLLoader.isProduction()` 时注册此魔符。Conflux 0.1.13 只准备了兼容；从 0.1.14 开始，NeoForge 提供默认开启的独立启动开关，让正常游戏也注册上游实现。游戏内中文名称沿用 Elemental 的“卡利亚圆阵”。

在 `config/ars_conflux-startup.toml` 中配置：

```toml
[optional_content]
enable_elemental_phalanx = true
```

该配置影响物品注册，修改后必须重启；客户端和服务器保持一致。设为 `false` 后保持上游的注册政策，并取消 Conflux 添加的配方。没有 Elemental 时不加载可选目标，也不生成配方。只改变 Elemental 注册魔符时的局部检查，不修改加载器的全局生产环境标志，不开启其他开发内容。

沿用上游三级魔符、175 基础 Mana、ID、名称、图标与实体，不复制其素材。新增 Ars 抄写台配方：追踪弹射物魔符、操纵精华、召唤精华、紫水晶碎片各一个；经验 160。配方只有在开关开启且对应魔符物品存在时加载，可正常供 JEI 和抄写台读取。

注册 `ars_elemental:glyph_carian_phalanx`，声明为由 Ars 原生轨迹驱动、创建时支付一次的载体。它不是环绕球的重复命中收费模式。

Elemental 会去掉原形态，给每个球构建命中编排；Core 在派生解析器上绑定方阵身份，保留来源附件。替换时，原 `EntityCarianPhalanx` 留作服务端轨迹载体，网络中的实体由提供者替换。原生实体继续计算阵形、五球上限、附近敌人选择、发射、追踪、命中和失效；Core 没有重写这些行为。关闭轨迹会丢弃原生载体，上游在再次施放时清理旧阵形条目。

载荷先根据余额限制新增球数，再由上游分配阵形位置。其余命中效果仍沿用原施法者，并按各提供者自己的规则付费。Elemental 1.20.1-0.6.8.0 不包含卡利亚方阵，该平台不会加载不存在的可选目标。

## 定向验证

开发依赖均被 Git 忽略：

- NeoForge：`libs/elemental/` 放 Elemental 0.7.10.4 和 TerraBlender 4.1.0.8；可同时开启真实 NEG 4.6.2。
- Forge：`forge/libs/elemental/ars-elemental-elemental1201.jar` 为 Elemental 0.6.8.0；可同时开启真实 NEG 3.2.1.1。

在对应构建目录运行：

```powershell
.\gradlew.bat :runGameTestServer -PwithElemental=true -PwithNeg=true -PgameTestNamespaces=ars_conflux_elemental
```

NeoForge 检查真实再次发射与方阵两组；Forge 检查真实再次发射一组。覆盖低余额／空余额、普通编排原数量、载体身份、来源继承、悬浮分槽、自动发射、追踪命中、创建费用不重复支付和关闭后再次施放。独立提供者夹具通过实际轨迹与生成入口运行，不依赖 Iron、Goety、BOMD。

0.1.13 本轮结果：NEG 与 Elemental 共存时，NeoForge 两组、Forge 一组通过；不装这两个附属时，NeoForge 四组 Core 回归、Forge 一组原生计费回归通过。双版本完成发行构建。方阵测试仅验证开发环境中注册的原生实现，不能证明发布环境提供此魔符；Forge 当前版本没有方阵，未宣称在该平台测试该内容。

这些服务端测试不等同于逐个原生载荷的客户端特效检查；不同法术外观、多人同步及上游所有装备的数值需要实际试玩确认。

### 0.1.14 注册验证

NeoForge 开发启动可追加 `-PsimulateElementalProduction=true`，加载仅属于 GameTest 的内部钩子，把 Elemental 注册处的原始生产环境检查模拟为 `true`。它不修改全局加载器标志，也不进入发行包。

```powershell
.\gradlew.bat :runGameTestServer -PwithElemental=true -PwithNeg=true -PsimulateElementalProduction=true '-PgameTestNamespaces=ars_conflux_registration,ars_conflux_elemental'
```

关闭配置后，用 `-PphalanxExpected=false -PgameTestNamespaces=ars_conflux_registration` 检查魔符、物品和真实配方都不存在。这样不会把开发环境天然注册误当作生产环境可用。

0.1.14 本轮结果：模拟发布环境且开关开启时，注册验证与两组 Elemental 行为测试全部通过（3 项）；关闭时，魔符、物品和配方均未注册（1 项）；不装 Elemental／NEG 时，Core 回归全部通过（4 项）。NeoForge 和 Forge 发行构建完成；Forge 仅同步版本号，不新增上游缺少的圆阵内容。
