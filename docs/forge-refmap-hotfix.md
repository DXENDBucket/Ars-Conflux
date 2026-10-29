# Forge 0.1.37 refmap 打包回归 / 0.1.38 修复

## 已确认的问题

用户提供的反馈截图指出，Forge 启动时 `ExecutionProjectilesMixin` 找不到 `ServerLevel.addFreshEntity`，同时报告 `No refMap loaded`。没有完整日志，也没有据此声称复现了反馈者的整套环境；但可以直接检查已经上传的发行包：

- Forge 0.1.37，CF 文件 `9354326`，SHA-256 `3ae866fc8037f239cc9fceed265254435f7913bb5c4f23b51b6e621ca1a88eaa`：配置声明 `ars_conflux.refmap.json`，实际 ZIP 内不存在该文件。
- Forge 0.1.36，CF 文件 `9345945`，SHA-256 `21d246224f8cbebd65d3a68ceb2e7d8b8813f30dfea5b43d7e3ebcf58c0e5f23`：包含该文件。它将 `addFreshEntity` 映射为 `Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z`。

因此这是确定的发行包缺陷。0.1.37 上一轮只比较了类文件，漏掉运行时映射；类相同不代表 Forge 发行包可启动。NeoForge 不使用这份 Forge SRG refmap，不受此次遗漏影响。

## 原因和修复

原打包日志显示 `compileJava FROM-CACHE`。MixinGradle 0.7.38 将原始 refmap 和额外映射写在 `build/tmp/compileJava/`，并在编译任务的后置动作中复制出供归档使用的 `ars_conflux.refmap.json`。这些文件原先不是已声明的编译任务输出。缓存恢复了 class，既没有恢复映射文件，也没有执行创建副本的后置动作，归档仍然成功。

0.1.38 将原始 refmap、重命名副本和额外 TSRG 文件全部声明为 `compileJava` 输出。首次编译与缓存恢复都能提供相同的归档输入。

- Forge `jar` / `jarJar` 打包时检查映射文件及上述生产方法映射。
- `verifyDistributions` 在重混淆后再次检查最终 Forge JAR。
- 发布工具检查每份 Mixin 配置所声明的 refmap 是否存在且可解析；新计划准备与旧计划真正上传前都执行，缺失时不发起网络请求。
- 游戏 Java 源码、法术数值、资源结算和附属译文内容未修改。

## 验证与边界

1. Forge 首次编译与重混淆打包通过。
2. 清空该工作树的 Forge 构建目录后，实际出现 `compileJava FROM-CACHE`，最终 `verifyDistributions` 通过。验证过程中发现只登记原始 refmap 不足以恢复重命名副本，已一并修复；早期失败日志保留供追溯。
3. 最终 refmap 与已发布的 0.1.36 解析结果完全一致；197 个 Forge class 与 0.1.37 逐字节一致。
4. 发布工具 23 项离线测试通过，覆盖缺失／损坏 refmap 拦截，以及哈希匹配的旧计划在上传前被拦截。真实 0.1.37 包被拒绝，0.1.36 与修复包通过。
5. NeoForge 双版本号对齐为 0.1.38，并完成打包检查。
6. 当前 Iron、Goety、BOMD、Cataclysm、Aquamirae、Mowzie 的本地 Forge 发行包均含各自声明的 refmap。这仅说明本次检查未发现相同遗漏，不等于重新测试其全部运行行为。

没有启动完整客户端、GameTest 或生产服务器；不声称已复现反馈者的完整启动过程。这里验证的是已确定的缺文件回归、缓存恢复路径与关键生产方法映射。构建产物和详细检查记录见 [验证记录](forge-refmap-hotfix-build.json)。修复交付时没有自动再次上传 CurseForge，也没有替换共享测试实例。


## 0.1.38 发布记录

2026-10-29，用户授权两个平台均上传，复用上述已验证产物。CurseForge Upload API 已接收项目 `1737308` 的 NeoForge 1.21.1 文件 `9358880` 与 Forge 1.20.1 文件 `9358881`；两项台账均为 `uploaded`，Release，审核通过后自动发布。API 接收不等于审核已完成，详细记录见 [发布回执](curseforge-conflux-0.1.38.json)。没有重新构建、重复启动游戏验证、改动共享实例或推送 Git。
