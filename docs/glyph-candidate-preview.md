# 候选魔符亮灭与周围友军

Conflux 0.1.26；NeoForge 1.21.1 / Forge 1.20.1。

Ars 书页试加候选后只保留错误位置 >= 候选末尾位置的错误。Core 和附属的一些验证按“动作 + 增强”整段判断，错误位置是前面的动作，因此 `弹射物 → 火球` 后第二个替换仍显示为亮；无载荷的等级增强、错误位置在效果上的其他限制也有相同问题。

只在书页候选验证入口处理：沿用当前完整 `ISpellValidator`，对比加入前后的错误多重集，将此次新增但指向前方的错误包装到候选位置。保留既有无关错误的原位置，移除旧错误的有效候选不受阻。已导入的重复载荷编排再次添加替换仍拒绝。整本书当前配方与施法校验不进入此包装。

`CandidateError` 直接委托原错误的 existing / adding 文本，不改为通用说明；不写入 `augmentDescriptions`，不改 `augmentingParent`。Iron 原生等级说明保持原来源。一次刷新范围内缓存相同验证器/前缀，嵌套与异常恢复线程状态。

Neo 的挂钩位于 `GuiSpellBook.validate(ISpellValidator)` 中第二处 validate 调用；Forge 位于 `validateGlyphButton`，两端客户端限定。原错误位置仍由服务器保存及执行时使用。

「周围友军」通过 `GlyphVisuals` 接入统一 GUI 绘制，保留现有 32px 中心，默认按 75% 比例缩小，外框按形态为紫色；受既有客户端配置控制。启动内容开关、Tier 3、费用与友军选取逻辑不变。

## 验证与开发预览

- 4 项纯策略测试：新增错误、既有无关错误、重复错误计数与修复旧错误。
- 客户端预览加载真实 Iron 0.17.21：火球后冰刺暗、强化亮；普通弹射物后强化暗；接伤害后冰刺暗、伤害强化亮；导入重复替换后再添加仍暗。
- 检查原服务器错误仍在位置 0，错误文本直接委托；Neo 额外检查 Iron 原生等级增强说明表未改变。Forge 的 Ars 4 没有 Ars 5 说明表。
- 预览强制加载原书页类以验证 Mixin 接线，用原生 `GlyphButton` 画出实际状态，并显示周围友军外观。没有在真实存档内人工遍历所有附属/所有书页。

运行 `:runClient -PglyphStatePreview=true`，忽略的 `libs/glyph-state-preview/`（Forge 使用本地 deobf 测试依赖）放入真实 Iron 附属与原生依赖。预览在独立菜单加载默认服务器配置，保存截图后退出；测试类与检查用依赖不打进发行包。

## 交付记录（2026-10-13）

- 实现提交：`cc9090f44eabd32fe51118d4921706718a6794e4`，标签 `v0.1.26`；仅 Core 变更，Iron 保持 0.17.21。
- 隔离工作区：`parallel-workspaces/glyph-state/Ars-Conflux`；Neo / Forge 各一次成功的客户端预览，均通过 6 个状态、文本委托、原错误位置与 Mixin 加载断言。Neo 客户端额外核对实际 Iron 增强说明表。
- Core 单元测试共 21 项通过，0 失败。未重新运行无关施法矩阵；未在完整整合包存档人工遍历所有组合。
- Neo 发行 SHA-256：`72d3adefd196a9adedb421d48392b2c299357d911671467ba59b3c3e0a02cbd7`。
- Forge 发行 SHA-256：`b9dee7df4f110bc0f0c4b853fd76811fdf591262ce7e370e793c39a8bf6de175`；重混淆产物。
- 源码包 SHA-256：Neo `55fedb3566d4a799eef57fae9c9675483d1ea165ef3ffccce583bcdc0b7b4c0f`；Forge `47cf8c2e899cc1b4bcd3f61c4d213cbd97a4b4734ee9203f03989ec1c65520b1`。
- 包内许可与预览代码排除已检查。最终包及源码包已复制主检出目录的对应 `build/libs/`，两个测试实例每个 Core modid 唯一、部署哈希匹配，旧 0.1.25 在实例 `.codex-backups/glyph-state-20261013/`。
- 两个客户端已自动退出，无本任务 JVM 残留。未改“万途归零”，未推送或上传。
- 主检出目录 `build/glyph-frame-preview/glyph-states-{neoforge,forge}.png` 保存实际预览；更新日志 `release-notes/conflux/0.1.26.md` 已准备。
