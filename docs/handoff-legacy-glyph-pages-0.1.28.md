# Conflux 0.1.28：Forge 法术书分页

## 范围与依据

- 用户反馈：Forge 1.20.1 多个附属与 NEG 共存时，魔符缺失、重复或分页异常；NeoForge 1.21.1 尚无明确复现。
- Core 基线：`b7b5f13857d5ed2231dae893e4560f5845639e2a`，本次分支 `fix/forge-neg-glyph-pages`，完成版本 `v0.1.28`。
- 独立工作树：`C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/conflux-neg-glyph-pages-20261019/Ars-Conflux`。
- 核对实际 Ars Nouveau 4.12.7 / NEG 1.20.1-3.2.1.1 发布 JAR。Ars 4 的普通与无限法术书分别实现同一套旧布局，不存在 Ars 5 的 `GlyphFormatter`。

## 原因与修改

旧实现先固定每 58 个条目切片，再布置分类标题和网格。标题和分类换行会消耗位置，装不下的切片尾部会被丢弃；后续页切换分类又可能把列号归零却不换行，造成重叠。已知魔符和默认初始魔符合并时也未去重。

- `LegacyGlyphPages` 按两个 6 列、7 行的原版页面计算实际布局。分类标题占一行，和首个魔符一起换边／换页；连续同类内容不重复插入标题。
- Forge 的 `LegacyBookGlyphPagesMixin` 同时作用于 `GuiSpellBook` 与 `InfinityGuiSpellBook`，使用实际页数和箭头状态。原生按钮、点击回调、搜索过滤、施法验证与排序来源保留。
- 显示条目按注册 ID 去重，不修改玩家已解锁数据或注册表；不新增对 NEG 或其他附属的硬依赖。
- 移除旧按钮时使用 `Screen.removeWidget`，同步清理交互、渲染和旁白列表，避免翻页累积旧条目。
- NeoForge 的原有 `GlyphFormatterPagesMixin` 不变；本次没有复现或断言其存在同一缺陷。

## 验证

一次收拢后执行必要检查，未跑完整 GameTest 矩阵。

1. Java 21，根目录 `:test :jar :sourcesJar --offline --console=plain`：27 个 JUnit 测试全部通过，包括新增 6 个旧布局测试和既有 4 个 Neo 布局测试。覆盖大批魔符、去重、分页边界、分类标题和首项同页约束、无重叠／无遗漏及空结果。
2. Java 17，`forge/`：

   ```powershell
   ./gradlew.bat :runClient :jarJar :reobfJarJar :sourcesJar -PglyphPagePreview=true -PwithNeg=true -PwithElemental=true --offline --console=plain
   ```

   使用 Ars 4.12.7、NEG 3.2.1.1、Ars Elemental 0.6.8.0、Iron & Nouveau 0.17.22、Iron 3.16.3。客户端真实注册表共 **232 个可显示魔符**，普通／无限法术书均为 4 页；全部条目按序出现一次，前后页布局、筛选子集和空结果、箭头状态、旧 widget 与旁白清理断言通过，正常自行关闭。

   这是无世界 GUI 测试夹具：仅绕过依赖玩家的书页构造函数，调用 Mixin 转换后的原生布局、页数和背景方法，用真实 `GlyphButton` 渲染。未模拟生存玩家的解锁过程，也未重新验证原生编辑／施法或声称完整存档人工测试。

   已查看普通书第 1、2 页截图，分类标题及实际图标无重叠、越界。四张截图（含无限书）位于 `forge/run/glyph-page-preview/screenshots/glyph-pages-{0..3}.png`。成功日志 `forge/build/glyph-page-preview-retry.log`；初次启动发现并修正多目标 Mixin 的 Shadow 注解必须显式 `remap=false`。
3. 最终 Forge 包已重混淆；检查新增类的 Screen／GuiGraphics 等调用已映射到 SRG。两平台包均带 MIT LICENSE，不包含测试夹具。Forge 内嵌 MixinExtras 范围仍为 `[0.4.1,)`。
4. 本次启动的测试客户端已结束，无所属残留 Java 客户端。Gradle daemon 保留，不停止其他任务进程。

## 交付

工作树产物：

| 文件 | SHA-256 |
| --- | --- |
| `build/libs/ars-conflux-0.1.28-mc1.21.1-neoforge.jar` | `dfc6299b7c7e0509cc77367aeac826067c8195c6b54425739f1841b0a1079770` |
| `forge/build/libs/ars-conflux-0.1.28-mc1.20.1-forge.jar` | `6c682abf3f832b0b3c400b40efc983328f653a9dd731e077795bb422258481f8` |

实现批次完成本地版本控制与构建，提交 `090d994`。未推送远程，也未改写共享游戏实例。未重新运行 Neo 客户端；其发行包版本同步，但分页运行时代码不变。

## CurseForge 上传

2026-10-19，用户明确要求上传后，直接使用上述已验证的发行 JAR，未重新构建或重复测试。发布更新说明为 `release-notes/conflux/0.1.28.md`，明确 Forge 修复范围及 Neo 同步版本。

- 项目 ID：`1737308`。
- NeoForge 1.21.1 文件 ID：`9252968`。
- Forge 1.20.1 文件 ID：`9252969`。
- 计划：`build/curseforge/20261019-142819-df2c6c85/plan.json`。
- 上传进程退出码 0，台账两项均为 `uploaded`，哈希与上表一致。
- `releaseType=release`、`isMarkedForManualRelease=false`，审核通过后自动发布。API 接收成功不代表已审核公开。

后续若用户仍能复现，优先索取具体 Ars／NEG／附属版本和书页截图，检查是否有额外 UI 模组接管网格；不要重新给每个法术附属补一套分页。
