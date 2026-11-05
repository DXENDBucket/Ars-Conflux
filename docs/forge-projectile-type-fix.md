# 0.1.53：Forge getTypeName 注入修复

Ars Nouveau 4.12.7 的 AbstractAugment 没有 getTypeName；Ars 5 的同名类才有。0.1.52 将 ProjectileGlyphTypeMixin 放在 common 并由两个加载器注册，导致 Forge 必需注入失败。这不是缺失 refmap 或用户配置问题。

将该文件移动到 NeoForge 的 src/main/java，并移除 Forge Mixin 注册。Forge 的 LegacyBookGlyphPagesMixin 已负责弹射物替换分类标题；边框、轮盘和法术语义不变。没有使用 require=0 掩盖注入错误。

Forge 的 jar/jarJar/verifyDistributions 校验现在同时拒绝包含或注册此 Ars 5-only Mixin 的包，保留既有正式环境 refmap 检查。

基线：2e9ca0c，工作目录 C:/Users/57991/Documents/MinecraftDev/Ars-Conflux，main。未修改附属隔离任务使用的固定 Core 副本。

验证：
- 对实际 forge/libs/ars-nouveau-forge1201.jar 与 libs/ars.jar 执行 javap，确认方法只存在于后者。
- Neo Java 21：jar、sourcesJar 成功，build/type-hook-neo.log。
- Forge Java 17、Forge 自带 wrapper：verifyDistributions、sourcesJar 成功，包含 jarJar/reobfJarJar，build/type-hook-forge.log。
- 检查最终两端 JAR 的 Mixin 注册和类：Forge 两者皆无，Neo 两者皆有；refmap 存在。
- 未启动游戏/测试服，未人工验证 UI；近期内存不足，不重复要求用户腾内存。
- 未部署、未发布 CF、未推送。

产物 SHA-256：

- `build/libs/ars-conflux-0.1.53-mc1.21.1-neoforge.jar`: `5bf2e5fd32f3f31161865d00ae564bea8392f90f67dd39cbe3a6f9d2d2d57172`

- `forge/build/libs/ars-conflux-0.1.53-mc1.20.1-forge.jar`: `e97171d2b591122a20dc3b18d763ae8fc1a09a422cdbfe66252a0b877f7a2817`

## 发布前同类复查

`tools/audit-mixin-entrypoints.py` 使用实际依赖JAR的类文件方法表，解析已注册Mixin的目标与方法选择器；Forge同时解析最终refmap。检查74处Forge／128处NeoForge入口，无缺失。常规Minecraft目标、无对应可选依赖类、未解析注解不能据此声称通过；这是入口存在性检查，不验证所有调用指令匹配与运行行为。详单在 `build/mixin-target-audit.json`。

其中近期新增的两版法术书分类、候选校验、轮盘预览与Neo类型名称入口均覆盖。无额外代码缺陷需要改变0.1.53产物。

## CurseForge 上传回执

2026-11-06，用户授权上传后，两份0.1.53发行包已被Upload API接收：NeoForge文件9456917，Forge文件9456918，项目1737308。计划 `build/curseforge/20261106-020717-6fd6b088/plan.json`。审核通过后自动发布；API接收不代表已经审核公开。本次未替换整合包内的Core。
