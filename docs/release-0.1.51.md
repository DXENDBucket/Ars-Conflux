# 0.1.51：Hex 客户端补丁归并（2026-11-04）

将旧 `Hex-Pre53-Client-Fix` 的服务类加载器修复纳入 NeoForge 客户端可选兼容。只处理 Hex pre53/pre64/pre67 中已审查的单参数 `ServiceLoader.load` 调用；未知版本、已变更调用或旧独立补丁存在时跳过。没有 Hex 或 Linker 时不增加前置；Forge 与服务端不登记此客户端 Mixin。详见 [适用范围与迁移](hex-ars-linker.md)。

## 验证

- Java 21，Ars 5.13.4，NeoForge 21.1.250；构建 NeoForge `jar`、`sourcesJar` 通过。
- 三项定向 JUnit 测试通过：版本／旧补丁避让、Hex pre67 实际字节码及上游修改后的拒绝、使用彼此隔离的真实类加载器复现服务丢失并调用实际 Mixin handler 验证修复。
- `compileGameTestJava` 通过，隔离菜单检查入口已编译。可用内存不足，未运行客户端或打开世界；未重跑整个游戏测试矩阵。
- 发行包根目录许可、pack.mcmeta、模组元数据、所有 Mixin 类存在性及未打入 Hex/Linker/测试检查类通过。
- Forge 本批无行为变更，仅共享版本号与公共插件中的可选分支；后续发布阶段以 Java 17 完成 `jarJar`、`reobfJarJar`、`sourcesJar`、`verifyDistributions`，生产映射检查通过，未运行 Forge 客户端。

## 万法归一部署

- Conflux 0.1.49 → 0.1.51，包含 0.1.50 的 NEG 分类修复。
- Hex pre64 → pre67，删除旧 `hex-pre64-client-fix-0.1.1-mc1.21.1-neoforge.jar`。
- 复查时 Ars 5.13.4、Linker 0.9.5.2.pre67、Iron Lib 2.3.0 已在实例，官方文件哈希一致，不重复替换。其他 Conflux 系列附属已与本地版本一致。
- 部署后 SHA-256 与顶层 modid 唯一性通过。遵照用户约定不备份旧 JAR，未修改存档、玩法配置或发布到 CurseForge。

构建、下载与部署明细保存在本机 `MinecraftDev/releases/wanfa-conflux-hex-20261104/`；上游 API 静态对照保存在 `inspection-cache/upstream-compat-20261104/`。

## CurseForge 接收记录

用户另行授权后，2026-11-04 Upload API 接收项目 `1737308` 的两份 Release 文件，审核通过后自动发布：

| 平台 | 文件 ID | SHA-256 |
| --- | --- | --- |
| NeoForge 1.21.1 | 9432158 | `1909e9813746c5de5f236bd46b77a1483714f536ff27b24f34ae5c99133ff55c` |
| Forge 1.20.1 | 9432159 | `0c7a211294c4ec0a89dd0057a3e139a15ae47e40d0be4982326c91a281b4f5d5` |

上传计划：`build/curseforge/20261104-174845-1f2afe72/plan.json`。API 接收不代表审核已完成；没有推送 Git。NeoForge 文件与已部署实例和 alpha.4 整合包内的文件哈希一致。
