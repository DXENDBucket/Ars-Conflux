# ars-conflux 0.1.19：Forge 内嵌库范围修复

外部报告：<https://paste.kostromdan.dev/mclogsarray/h1DbRK6a47Tl0E0eMn4y5pBc>，崩溃文本 `/mclogs/y8VvLkA`。原报告使用 Conflux 0.1.11、Iron & Nouveau 0.17.14；其 MixinExtras `[0.4.1,0.5)` 与其他模组要求 `[0.5.5,)` 无交集，导致 Forge JarInJarDependencyLocator 拒绝启动。另有多项缺失前置，修复此范围不代表报告者整合包全部问题解决。

仅将 Forge `jarJar.ranged` 改为 `[0.4.1,)`，继续编译／自带 0.4.1 回退库，但允许整合包选择新版。遵循上游无次版本上限形式：<https://github.com/LlamaLad7/MixinExtras#setup>。Java 行为不变，两平台版本号同步。

## 验证

- Neo 和 Forge 发行／源码包构建通过；Forge 执行 `jarJar reobfJarJar sourcesJar`，验证最终重混淆包的版本、MIT 和内嵌库元数据。
- 工具 `Ars-Conflux/tools/check-forge-jarjar-ranges.py` 和 `ForgeJarJarRangeCheck.java` 调用实际 Forge JarJarSelector 0.3.19，读取两个最终包：无其他要求时选 0.4.1；加入要求 0.5.5 的模拟提供者后选 0.5.5；恢复旧上限按预期失败。负例日志的 ERROR 是预期。
- 日志 `Ars-Conflux/build/forge-jarjar-range-check.log`；使用本地缓存，不启动服务器。模拟提供者只验证依赖选择，不能称为已在游戏中运行 0.5.5。
- 不重复跑战斗矩阵：Core 0.1.18 的双平台 API 及灾变/BOMD 回归仍适用，此修复不改 Java 行为。Iron Neo 构建另跑既有 JUnit。
- 未在报告者完整整合包人工启动；仍需补齐其缺失依赖。

起点 Core `dab8b9f`（0.1.18），保留 NativeSpellDamage。工作树 `parallel-workspaces/conflux-native-damage/Ars-Conflux`，分支 `feature/native-spell-damage`。

## 产物 SHA-256

- `build/libs/ars-conflux-0.1.19-mc1.21.1-neoforge.jar`：`f2e96a328a17f65c04d4cda8d1d4eb9796e227718f8c0c899e1a18e16f7e408c`
- `build/libs/ars-conflux-0.1.19-sources.jar`：`616cfa30cd1f383e662df398e6dcf20b6705b217c345ea95c40f65d857bff6bf`
- `forge/build/libs/ars-conflux-0.1.19-mc1.20.1-forge.jar`：`8b3dcb357bc6cde4649cc7778387e1ab4d8a3080504d3a635175e3d16bdcac67`
- `forge/build/libs/ars-conflux-0.1.19-sources.jar`：`5674314a9926fdd7871d1d4b9d7eb3005a487303c281a0d2e0d7f361d7183a9e`
