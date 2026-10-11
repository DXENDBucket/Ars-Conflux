# 构建

两个入口共享 `common/` 及根目录 `VERSION`，发行的是两个独立加载器 JAR。

## NeoForge 1.21.1

使用 Java 21。把以下开发依赖复制到根目录 `libs/`，不要提交或打进发行包：

| 文件 | 版本 |
| --- | --- |
| `ars.jar` | Ars Nouveau 5.13.1 |
| `curios.jar` | 与该 Ars 版本兼容的 Curios NeoForge |
| `geckolib.jar` | 与该 Ars 版本兼容的 GeckoLib NeoForge |

运行 `gradlew.bat :jar :sourcesJar`，安装 `build/libs/ars-conflux-0.1.12-mc1.21.1-neoforge.jar`。

独立验证使用 `gradlew.bat :test :runGameTestServer`。测试不需要 Iron、NEG 或 Ars Elemental。

触发计费定向验证：`gradlew.bat :test :runGameTestServer -PgameTestNamespaces=ars_conflux_billing`。一项隔离 GameTest 内检查关闭开关、起手和命中收费、重复命中、空蓝、创造免费与真实嵌套环绕，结束恢复配置。

## Forge 1.20.1

使用 Java 17。在 `forge/libs/` 放入 Ars Nouveau 4.12.7、Curios 5.14.1、GeckoLib 4.8.4、Patchouli 85 的 Forge 1.20.1 JAR。文件名分别是 `ars-nouveau-forge1201.jar`、`curios-forge1201.jar`、`geckolib-forge1201.jar`、`patchouli-forge1201.jar`。

进入 `forge/`，运行 `gradlew.bat :jarJar :sourcesJar`。安装 `build/libs/ars-conflux-0.1.12-mc1.20.1-forge.jar`；它包含 MixinExtras 并完成重映射。不要把普通开发 JAR 作为另一份模组一起安装。Gradle 8.8 可以由 JDK 21 启动，但 Forge 编译与游戏运行仍使用 Java 17 工具链。

该目录的 `gradlew.bat :runGameTestServer` 独立验证 Ars 4 原生资源池、开关行为、嵌套环绕和创造免费，不依赖 Iron。

Hex-Ars Linker 的可选运行环境与定向验证统一放在 Core，参见 [准备依赖与执行命令](hex-ars-linker.md#定向验证)。默认构建和运行不加载这些依赖。

## Iron 开发联调

将仓库与 Iron & Nouveau 放在同级目录。Iron 的 NeoForge 根构建默认从 `../Ars-Conflux` 引入 Core；Iron 的嵌套 `forge/` 构建默认从 `../../Ars-Conflux/forge` 引入。不同目录可以传 `-PconfluxCheckout=路径`。Iron 的 `tools/fetch_dependencies.py` 可按锁定版本准备两个仓库的开发依赖，`tools/build_all.ps1` 可生成两边的发行包。

编译期是 Gradle 项目依赖，运行和发布时仍是两个分开的模组 JAR。游戏内必须同时安装匹配版本的 Core 和 Iron & Nouveau。

使用冒号限定根任务，例如 `:runGameTestServer`；不限定的任务选择器会同时运行子项目同名任务。组合联调可用 `-PgameTestNamespaces=irons_nouveau_conflux` 只执行迁移回归。


## 0.1.10 定向验证

主动施法用例由消费者开发工程承载：Goety NeoForge 在 Iron / NEG 共存环境通过 5 项，Forge 无 Iron 环境通过 4 项，包含实际 Ars 工具的准备与释放、混合提供者一次吟唱、取消／失败不登记冷却，以及原生 Goety 冷却共享。Iron NeoForge 另通过 2 项吟唱和 3 项冷却回归（含纯 Ars、散射、持续触发和弩弹药）。未重跑历史全量法术目录，未启动客户端；两家原生 HUD 的实际外观需要游戏内核对。
