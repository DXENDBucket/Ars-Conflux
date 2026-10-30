# Ars Conflux 工作约定

先读 [开发与测试流程](docs/development-workflow.md)，交接使用 [任务模板](docs/agent-handoff-template.md)。

- Conflux 是跨附属的中立 API；原模组特有语义留在各附属，整合包流程和 AI 留在整合包核心。
- 正在做附属任务的 agent 不修改本仓库受版本控制文件。需要 API 扩展时记录需求，交给独立 Core 任务。明确以 Core 为目标的任务按用户授权推进。
- 附属并行构建使用各自固定提交的 Core worktree，避免共享 `build/`、`run/`。Core 副本源码只读，自己的忽略构建目录可写。
- 根目录维护 NeoForge 1.21.1 / Java 21，`forge/` 维护 Forge 1.20.1 / Java 17，共享版本来自 `VERSION`。
- 用户希望减少频繁测试。按实际改动选必要验证，收拢后执行；通过后不无理由重复全矩阵。
- 不把服务端 GameTest 或资源路径检查说成客户端外观已验证。
- 只提交自己本任务的文件；外部推送、发布和共享实例部署依任务授权，不由本约定自动授权。

- School declarations are optional and per effect/payload, not per whole CastContext. Preserve native Ars School membership and DamageSource; only explicit namespaced weight declarations participate. Core provides no preset schools. Default damage rules off. Data reload is atomic, empty overrides mask code defaults, and delayed native payloads carry immutable snapshots through NativeSpellDamage overloads.
