# Changelog

## 0.1.0

- 从 Iron & Nouveau 抽出独立的共享执行核心，两端共用公共源代码和版本号。
- 提供通用资源账户、共享物理池预留和触发成功提交 API；提供公开的资源池身份。
- 提供执行会话、作用域、命中接续和生命周期调度，不引用 Iron 原生类型。
- 提供可注册载体、替换载荷、共享 Ars 轨迹和对称散射预算。
- 集中 Ars、NEG 与 Ars Elemental 的公共载体钩子、触发方向与组合验证。
- Iron & Nouveau 0.17.0 接入，两端魔符、配置和玩家学习数据 ID 保持原值。
- NeoForge：4 项资源单元测试、4 项不安装 Iron/NEG 的 Core GameTest、7 项 Iron 迁移 GameTest 已通过。
- Forge：发行构建、重映射及 4 项 Iron 迁移 GameTest 已通过。
- 两端各增加 1 项独立提供者边界验证：第三方载荷可接 Iron 效果，组合规则不要求双方专门适配。
- 原版 Ars 默认计费未改动；Goety 接入尚未实现。
