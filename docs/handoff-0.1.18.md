# Conflux 0.1.18 原生法术伤害抽取

- 工作树：`parallel-workspaces/conflux-native-damage/Ars-Conflux`，分支 `feature/native-spell-damage`。
- 起点：`356c24a`（0.1.17），保留已有分页修复及其余 API。
- 功能：中立 NativeSpellDamage API、瞬态附件、明确的同步作用域、两平台 Ars 属性／Pre–Post 桥接、共享重入保护和卸载清理。附属只保留原生钩子及特有上下文查询。
- 未自动接入 Iron/Goety，不改变原生 Ars 效果；Core 无法术附属硬依赖。
- Neo：`runGameTestServer build -PgameTestNamespaces=ars_conflux_damage`，3/3 新 API GameTests + 既有 JUnit 通过。
- Forge：`runGameTestServer jarJar reobfJarJar sourcesJar -PgameTestNamespaces=ars_conflux_damage`，3/3 同一套 API GameTests 通过，发行包已重混淆。
- 各自日志：`build-native-damage-neo.log`、`forge/build-native-damage-forge.log`，在忽略目录内，不发布。
- 发行包核对 MIT、版本、Java 字节码、API／Mixin 类和无附属打包；Forge 仅按原规则嵌入 MixinExtras。
- 客户端外观未人工测试；这是服务器伤害通路抽取，未修改渲染。同步作用域不传播到未来 tick，需由子实体／会话显式继承上下文。

## 产物 SHA-256

- `build/libs/ars-conflux-0.1.18-mc1.21.1-neoforge.jar`：`5601e0784905e1fe88a74b6fed73e6badc2f6d674dfec66b607e53f3500b0a08`
- `forge/build/libs/ars-conflux-0.1.18-mc1.20.1-forge.jar`：`49ca0e186b698fbc24eecb8224f0985b962749fdeeb5473d8b0c72a4a28a4704`
- `build\libs\ars-conflux-0.1.18-sources.jar`：`616cfa30cd1f383e662df398e6dcf20b6705b217c345ea95c40f65d857bff6bf`
- `forge\build\libs\ars-conflux-0.1.18-sources.jar`：`0a1bec7da332473395f358f558010230b0d8c9d84fde10c2e63590aabfc7559b`
