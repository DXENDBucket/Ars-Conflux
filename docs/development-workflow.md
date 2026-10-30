# Ars Conflux 与附属的开发、验证和交接流程

这份文档记录已采用的工程约定，供人、Codex 和其他开发 agent 共用。项目 `AGENTS.md` 是入口；当前任务和用户新指示优先。版本、提交和测试结果以当前仓库为准，不把一次历史成功当作后续修改的验证。

## 1. 项目边界

| 项目 | 职责 | 常用布局 |
| --- | --- | --- |
| Ars Conflux | 跨附属的施法上下文、弹射物适配、资源结算、执行会话、主动施法协调、展示与校验 API | 根目录为 NeoForge；`forge/` 为 Forge |
| Iron / Goety / BOMD / Cataclysm & Nouveau | 原模组能力适配、魔符、获取配方、配置和本体语义 | 共享逻辑在 `common/`；平台接线在各平台目录 |
| Ars Instinct | 可复用施法控制器/Goal、消费能力提示、默认关闭的按类型/标签接入 | 根目录 NeoForge，`forge/` Forge，共享 `common/` |
| 整合包核心 | 整合包流程、世界生成、物种配装/数值/安装策略与特殊行为、开局物资等 | 独立项目，不放进通用附属 |

普通附属任务使用既有 Conflux API，不改 Conflux 源码、资源、版本或构建配置，也不补附属之间的直接依赖。确实需要新 API 时记录具体需求和最小复现，交给协调者另开 Core 任务；先推进不依赖该 API 的部分。

适配原模组能力时，优先调用原模组真实的实体、生命周期和公开方法。不要为了让代码能运行而复制半套施法实现，或遗漏客户端效果。许可证必须按各原模组分别检查；“运行时引用”“复制素材”“复制实现代码”是不同的行为。

## 2. 开始任务

1. 读取本项目 `AGENTS.md`、`README.md`、`VERSION`、`build.gradle`、`settings.gradle` 和相关现有适配文档。
2. 检查 `git status --short`、当前分支和 `git worktree list`，确认自己的目录及允许修改范围。不要接管其他 agent 的工作树。
3. 记录固定的 Conflux 提交、目标 MC/加载器、本体 JAR 版本、要实现的能力和哪些验证确实需要。依赖版本以实际构建和 JAR 元数据为准。
4. 先找一个结构相似的既有适配作为参考，再核对原模组发布 JAR。GitHub 分支源码只能辅助阅读，不能替代对实际版本的检查。

当前双版本方案使用相同仓库的 `common/` 加 `forge/`，没有采用 Architectury 或 Stonecutter。不要误把旧 Iron 1.20.1 分支工作树当成当前同步版本。

## 3. 并行开发的隔离

可以让多个 agent 同时开发不同附属。每个任务至少有独立的：附属工作树、分支、固定提交的 Conflux 构建副本、`build/`、`run/`、测试输出和可写测试目录。

```text
parallel-workspaces/
  task-a/
    Ars-Conflux/       # detached、固定提交；源码视为只读
    Goety-Nouveau/     # agent A 的工作树
    test-instance/    # 独立手动测试落地位置，需自行准备完整客户端环境
    workspace.json
  task-b/
    Ars-Conflux/       # 同一基线的另一构建副本
    Cataclysm-Nouveau/
    test-instance/
    workspace.json
```

仅“不改 Core 源码”还不够：现有 Gradle 多项目构建会写入包含的 Core 的构建目录。多个附属构建若指向同一个 Core 检出目录，仍可能覆盖彼此的类文件、JAR 和缓存状态。因此并行任务使用独立的 Core worktree，保持该副本的受版本控制文件不变，允许 Gradle 写入它自己的忽略目录。

`tools/prepare-addon-workspace.py` 可以准备上述布局。默认只打印计划；传入 `--apply` 才创建工作树。它不创建 agent、不部署到共享实例、不修改 Core 实现，也不自动清理目录。

```powershell
python ./tools/prepare-addon-workspace.py `
  --addon ../Goety-Nouveau `
  --core . `
  --workspace ../parallel-workspaces/goety-next `
  --branch agent/goety-next

# 确认选用的提交和目录后，用相同参数加 --apply 执行。
```

默认使用两个仓库的当前 HEAD，并解析成完整提交；可用 `--core-ref` 和 `--addon-ref` 固定其他已存在提交。不要通过拉取最新 Core 解决基线不一致。工具复制本地忽略的 `libs/` 和已知本地编译依赖目录；不会下载、修改或打包原模组 JAR。

同一个物理电脑上不要同时开多份完整客户端或大量 GameTest JVM。代码可并行，重型测试错峰进行。默认不要给每个 agent 分配独立 Gradle 用户缓存，以免重复下载和增加磁盘占用；发生缓存锁问题再具体处理。

## 4. 一个能力的适配核对

| 边界 | 要检查的内容 |
| --- | --- |
| 实际入口 | 原版调用位置、构造参数、实体注册、本体配置、潜行和副手装备修饰 |
| 原生行为 | 伤害、状态、爆炸、延迟、持续触发、是否穿透/反弹、召唤和数量限制 |
| 两端表现 | 服务器实体、同步字段、原版实体事件/数据包、客户端渲染器、动画、粒子、声音 |
| 上下文 | 施法者、实际执行者、伤害来源、命中实体/位置、飞行方向和所在维度 |
| 资源与施法状态 | 实际触发扣费、重复触发扣费、吟唱/冷却/移动影响、创造模式、本体默认行为 |
| 生命周期 | 失败、空蓝、所有者死亡、换维度、区块卸载、自然结束和取消后清理 |
| Ars 组合 | 接续效果、增强数值、散射预算、环绕、溅射、追踪、回响、传播器生成的子弹射物 |
| 玩法与呈现 | 真正的获取配方、所属形态/增强/效果类别、模组内顺序、本地化和配置描述 |

不是每个能力都需要重新验证全部项目，只对实现涉及的边界执行有意义的检查。反复出现过的漏项是“服务端伤害已能发生，但模型、动画、粒子、装备修饰或移动限制没有接上”。

优先复用 Core 的 `CastContext`、`ProjectileForm`、`CarrierProfile`、`ArsTrajectory`、资源账户/事务、`ExecutionSession`、主动施法和展示 API。具体签名读取固定版本源代码，不凭文档猜测。

选中实体不等于把它变成施法者或付款者。延迟、嵌套和命中续接要保留上下文；跨维度续接使用执行当前的世界。原生实体的运动和 Ars 运动只能有一个实际驱动者，避免双倍位移/命中。Mixin 尽量只对本附属 opt-in 的实例生效。

## 5. 测试强度与命令

用户明确希望减少频繁测试。按改动风险选最小有效范围：普通文字/配方/显示调整做针对性检查；新实体载荷、扣费、生命周期、Mixin 和跨端行为才需要集成测试。一次收拢同批修改后验证；只有失败、新修改或未解决的疑点才重跑。

### NeoForge 1.21.1

使用 Java 21 和仓库自己的 Gradle wrapper。示例在附属工作树根目录执行：

```powershell
$env:JAVA_HOME = '你的 JDK 21 路径'
./gradlew.bat :compileJava :processResources --offline --console=plain
./gradlew.bat :runGameTestServer --offline --console=plain
./gradlew.bat :build --offline --console=plain
```

这三步不是每次都要运行的固定套餐。`runGameTestServer` 已含编译；确认需要它时可合并打包任务到同一次调用。优先使用已缓存依赖；离线缺依赖时说明缺什么，再有针对性地联网获取。

### Forge 1.20.1

使用 Java 17，在同一工作树的 `forge/` 执行它自己的 wrapper。只验证平台存在的功能：

```powershell
$env:JAVA_HOME = '你的 JDK 17 路径'
./gradlew.bat :runGameTestServer --offline --console=plain
./gradlew.bat :jarJar :reobfJarJar :sourcesJar --offline --console=plain
```

发布用 Forge 重混淆后的最终产物；不要部署 dev JAR 或未重混淆 JAR。两端共享 `VERSION`，实际移植范围记录清楚。Cataclysm 附属从 0.1.1 起也提供 Forge 1.20.1；平台差异和验证边界见其 `forge/README.md`。

### 可选联动

只打开改动涉及的联动参数，不每次跑全矩阵。当前参数由各项目 `build.gradle` 决定，例如 Core 的 `withNeg`、`withElemental`、`withHexLinker`、`withControleAdditions`，Goety 的 `withIron`，BOMD 的 `coexistence`。测试 namespace 默认值也可能随参数改变，使用前检查构建文件。

缺少可选模组时应可正常启动。涉及类加载、Mixin 目标或客户端类引用的修改，需要验证专用服务器/最小环境；涉及跨附属续接时，再补对应共存环境。

GameTest 通过只能证明测到的服务端行为。没有开客户端就明确写“客户端外观未人工验证”，不要把资源路径检查等同于画面验证。

## 6. 打包和部署

1. 核对加载器/MC 版本、modid、版本、依赖声明、Mixin 配置和所需许可文本。
2. 排除 `libs/`、`.upstream/`、测试世界、日志和开发用依赖。允许的辅助库是否 jar-in-jar 按该项目现有配置判断，不机械禁止所有嵌套 JAR。
3. 本体素材的路径引用优先；复制实现或资源前单独确认许可边界。Cataclysm 当前坚持纯引用，不包含本体任何素材或源码实现，其 `verifyDistributions` 必须通过，二进制包和源码包都查。
4. 部署前看实际测试目录和已有 JAR，保证每个 modid 只有一个有效版本。核对复制后的哈希。

Forge 发行包还必须包含每份 Mixin 配置声明的 refmap，并核对实际生产环境的 SRG 方法映射。类文件一致不能代替这项检查。MixinGradle 的编译输出包括原始 refmap、打包用的重命名副本和额外映射文件；三者都应声明为编译任务输出，确保 `FROM-CACHE` 时一起恢复。修改这段构建接线后，用一次清空构建目录后的缓存恢复打包验证。Core 的 Forge `verifyDistributions` 会检查最终发行包，发布工具也在准备与上传旧计划时检查声明的 refmap；缺失时不发起上传。

并行 worker 不写共享“万法归一”或 Forge 测试实例。提交 JAR 路径、哈希、基线和测试记录，由一个指定协调者串行部署并验证共同环境。存在不兼容的 Core 基线时先统一，不偷偷换共享 Core JAR。

关闭本次启动的测试服务和客户端，检查残留进程。并行时不要 `Stop-Process` 所有 Java，也不要随便 `gradlew --stop` 停掉别人的 Gradle daemon；以本次启动的 PID/目录判断归属。

## 7. Git 与交接

只提交本任务文件，先检查差异和 `git diff --check`，不把别人的变更带进提交。功能版本修改用项目 `VERSION`；仅工作流文档无需提升模组版本。未经当前任务授权不推送、不创建公共仓库、不发送外部消息。

每批交付说明：最终行为、必要验证及其实际结果、未验证的边界、构建产物、提交和版本。并行任务还提交 Conflux 固定完整 SHA，以及该副本受版本控制文件未变化的结果。不要把这份流程当作继续发布或修改 Core 的授权。

推荐使用 [agent-handoff-template.md](agent-handoff-template.md) 记录任务状态。长期原则更新本文件；版本和当前任务状态放交接记录，避免每个附属复制一份越来越不同的流程。

## Forge 内嵌依赖范围

MixinExtras 可自带最低可用版本，但 Jar-in-Jar 使用 `[0.4.1,)`，不要无依据加 `<0.5` 这类上限。多个模组的版本范围无交集时会在预加载阶段退出，增加独立新版 JAR 也无法修复旧包的范围。涉及此配置时，用 `tools/check-forge-jarjar-ranges.py <Core Forge发行包> <Iron Forge发行包>` 调用实际 Forge 选择器；要检查最终包的 Jar-in-Jar 元数据。
