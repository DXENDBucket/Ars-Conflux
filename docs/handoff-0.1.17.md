# 0.1.17 分类分页修复交接

- 工作树：`C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/conflux-glyph-pages/Ars-Conflux`；分支 `fix/glyph-page-boundaries`。
- 基线：`b24f18d72e64a902ff6c966040b0b83ea3cc6226`（0.1.16）。
- 明确的 Core 任务：修复大列表分类图标的分页边界；不修改附属固定 Core 副本。
- 对实际测试实例 Ars Nouveau 5.13.3 JAR 和开发 5.13.1 核对：`GlyphFormatter.buildPages` 在分类行补齐前决定页边界，补齐跨过 98 后仍将图标加到旧页。
- 保留原生分类／魔符按钮及排序，仅用纯格位计算替换标准两栏布局；非标准网格不接管。
- NeoForge 新增客户端 Mixin；Forge Ars 4 没有 GlyphFormatter，不增加该 Mixin，版本同步 0.1.17。
- `build-glyph-pages.log`：JUnit 12 项，失败 0；含 4 项新增分页回归，NeoForge binary/sources 已构建。
- `forge/build-glyph-pages-forge.log`：Forge jarJar + reobfJarJar + sourcesJar 通过。
- 没有启动客户端；实际翻页画面仍需用户确认。
- 无推送；提交通过 v0.1.17 标签定位。

## 产物 SHA256

- `build/libs/ars-conflux-0.1.17-mc1.21.1-neoforge.jar`：`6dde2ad9c72984c3bd3115db161b9c7e8f22884c524c6b09c05649e8152a5b46`
- `build/libs/ars-conflux-0.1.17-sources.jar`：`f85d7adfd1d9367aedc573df83024e677b6137a9b5dffcaac1f22bc6eaa5ab3d`
- `forge/build/libs/ars-conflux-0.1.17-mc1.20.1-forge.jar`：`fa24a7c969e3b60d2b2a192f63f3fe342258d76d8de255262c453a751d1fcd24`
- `forge/build/libs/ars-conflux-0.1.17-sources.jar`：`d466564b33bea76ae20c7fb9beca54021081a8d0b602a43d8794a45c8277fa3c`
