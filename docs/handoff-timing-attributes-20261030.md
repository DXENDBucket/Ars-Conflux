# 通用时间属性交接（2026-10-30）

## 最终范围

- Conflux 0.1.40：同步注册两种属性；统一准备汇总后缩减；公共冷却存储在写入时缩减，保留原始截止时间恢复入口。
- Iron & Nouveau 0.17.31：原生 CDR／武器倍率／单发重施分摊后缩减，兼容原生自身和非玩家绑定工具。
- Goety & Nouveau 0.1.5：原生冷却后缩减、独立聚晶准备和棱镜准备缩减；仅在原生聚晶作用域中捕获自定义冷却写入，并避免二次缩减。
- 默认加成 0，没有装备、配方或永久奖励变更。原版 Ars 即时施法、输出时长、脉冲和 Delay 间隔保持原规则。

API、调试指令及精确边界见 [timing-attributes.md](timing-attributes.md)。

## 基线与工作区

协调者在各仓库现有干净的主检出目录串行完成，无附属 worker 并发构建。起点为 Core `d4a81d6d259e614a119475be3a135d4916325b46`、Iron `2364c2eca9fffa007c255afd286714000f731822`、Goety `d6760f1`。固定的最终 Core SHA、各仓库最终提交及所有 JAR SHA-256 记录在本机 `releases/timing-attributes-20261030/manifest.json`。

版本号跳过已撤回的 Core 0.1.39／Iron 0.17.30；没有重新引入其弹射物存活和轨迹改动。Core 新版本通过公共冷却存储自动覆盖现有灾变、BOMD、海灵、Mowzie 和传奇怪物，无需让这些附属依赖 Iron。

## 定向验证

| 工程 | 范围 | 日志 |
|---|---|---|
| Core NeoForge / Forge | 各 2 项：真实属性、快照、截止时间、混合准备、纯 Ars 和边界 | `build/timing-gametest.log` / `forge/build/timing-forge.log` |
| Iron NeoForge / Forge | 各 3 项：实际原生分摊、事件与覆盖、通用缩减和作用域 | 同上 |
| Goety NeoForge / Forge | 各 7 项：原生生命周期、资源、准备缩减、自定义冷却及作用域 | 同上 |

最终通过状态和发行包检查结果记录于发布目录 manifest。Core Forge 同时执行 `verifyDistributions`；所有 Forge 最终二进制取重混淆后的平台发行 JAR，保留声明的 refmap。没有启动完整图形客户端验证属性提示或进度条外观。

## 交付

只部署 NeoForge 的 Core、Iron、Goety 三个更新到“万法归一”；旧 JAR 在实例的 `.codex-backups` 中保留。Forge 产物准备就绪但不替换其他人工测试实例。本批本地提交，不自动上传 CurseForge 或推送远程。
