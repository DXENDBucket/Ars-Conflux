# Conflux 0.1.16 交接

## 范围与实现

- Core 独立工作树：`C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/conflux-allies/Ars-Conflux`；分支 `agent/conflux-allies`。
- 起点：`3eccd65fc569cd43ef960ef3eee77963ef201ac8`。本轮明确修改 Core；未修改其他附属源码。
- 双平台：NeoForge 1.21.1 / Java 21、Forge 1.20.1 / Java 17；共享版本 0.1.16。
- 新中立 API：`Allies`，原生队伍与所有权、对称的扩展提供者注册。
- 可选三级形态：周围友军，默认 8 格，扩散每份 +3，默认 30 Mana、抄写经验 160，排除自身。
- 独立目标解析器保留施法者、伤害所有者、来源附件和资源账户；常规 Ars／附属支付规则保持。
- 开关：`enable_nearby_allies=false`；Neo 的 `enable_elemental_phalanx` 也改为 false。客户端／服务器一致，需重启。
- Forge 无 STARTUP 类型：注册前只读自己的配置文件，后续由 COMMON 负责校验／写入。
- Goety 实际 JAR 的 Owned 实现 OwnableEntity、重写原生友军方法；常见仆从由 Core 基础判定覆盖。专有玩家盟友名单未额外接入，扩展接口已留。
- 新透明 32×32 像素图标由固定代码绘制；本轮不复制外部图片或实现。

## 验证

- Neo 启用：`:runGameTestServer :jar :sourcesJar -PgameTestNamespaces=ars_conflux,ars_conflux_allies -PalliesExpected=true`，7 项通过（4 项既有 Core 回归＋3 项友军定向检查）。
- Neo 关闭且加载真实 Elemental：`:runGameTestServer -PwithElemental=true -PsimulateElementalProduction=true -PphalanxExpected=false -PgameTestNamespaces=ars_conflux_allies,ars_conflux_registration`，4 项通过。确认生产注册门控、物品及配方不存在，API仍可用。
- Forge 启用：`:runGameTestServer :jarJar :reobfJarJar :sourcesJar -PgameTestNamespaces=ars_conflux_allies -PalliesExpected=true`，3 项通过，真实抄写配方加载、友军筛选、扩散及两个后续效果续接通过。
- 每次使用 `--offline --no-daemon --console=plain`。日志为工作树 `build-allies-tests.log`、`build-allies-disabled.log`、`forge/build-allies-forge.log`，均忽略。
- 中途失败为新测试缺空模板、未注册记录效果默认不启用，以及 Forge 抄写配方格式差异；均已修正并通过相关重跑。
- 发行和源码包静态核查：含 MIT，无测试类、Goety 类、其他附属实现。Forge 仅包含既有获准的 MixinExtras jar-in-jar。
- 客户端新图标／粒子、多人画面未人工验证；未运行实际 Goety 仆从矩阵，原生接口核对不标为实测。

## 交付

- 本地提交／版本标签：`v0.1.16`，不推送远程。
- 将 Neo 发行包部署到“万法归一”并在其 `config/ars_conflux-startup.toml` 中显式启用两项；旧包／配置保留于 `.codex-backups/ars-conflux/pre-0.1.16/`。
- Forge 完成发行构建，未改其他整合包配置；两端产物复制回主检出目录的忽略 build/libs。
- 发行哈希：
  - `build/libs/ars-conflux-0.1.16-mc1.21.1-neoforge.jar`：`2aad5052a2ed162408dd18f712aabb0c37e15f76f236c2473be69ce7eeea39ab`。
  - `forge/build/libs/ars-conflux-0.1.16-mc1.20.1-forge.jar`：`eb2b05b743805b3e86b89ddb9d0440cc7e6cffc155d351f7f220de7ffb504e2e`。
  - `build/libs/ars-conflux-0.1.16-sources.jar`：`1a5fe024c418a7f75cc90e1aff9421c9e92366e4074cce0318457cb074353ad9`。
  - `forge/build/libs/ars-conflux-0.1.16-sources.jar`：`75dba75989c7cc4cb5e424c9d73736f5d16254fe8ec1dd5af0c716d9143dd8ac`。
