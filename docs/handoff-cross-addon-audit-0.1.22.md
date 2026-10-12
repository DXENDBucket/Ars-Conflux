# 横向审计修复交接（2026-10-13）

Core 0.1.22；各附属固定运行代码提交 `a8a2e26392cd95532375b7e23b90e9740bf051b1`。运行代码在 `fix/cross-addon-core`，各附属在独立 `fix/cross-addon-audit` 分支完成。共享实例由协调者统一部署，未推送或发布。

## 公共改动

- `CastRejection`：提供者静默查询，Core 统一选择最长剩余冷却并提示。
- `MovementLimits`：同一输入事件按最严格限制合成，保留原生属性影响。
- `ActiveCastPolicy`：中立入口策略，默认行为不变；Hex 三种主动入口默认检查冷却和准备，可独立关闭。
- 普通 Mixin HEAD 注入替代仅新版支持的 WrapMethod 参数强转，验证 Forge 0.4.1。

## 已完成的附属修复

Iron 局部资源账户不向后续其他附属泄漏；Goety / BOMD 执行授权接入公共友军关系；BOMD 地潜朝实际宿主方向；Mowzie 位置爆发不改变付款者状态，太阳光束采用短期移动限制；六附属共同冷却反馈。结果冷却与持续会话保留原设计。

## 定向验证

Core NeoForge 和 Forge 各 6 项真实 Hex-present GameTest 通过，包含常规/指定位置/实体操作、独立提供者、回调 VM、归属计费、冷却拒绝和吟唱/冷却四组合；Neo JUnit 移动合成单元验证通过。日志 `audit-options-neo.log`、`forge/audit-options-forge.log` 在忽略的本地工作树。

同批 Neo 独立用例：Iron 3、Goety 4、BOMD 10、Mowzie 8，全通过；Iron 使用 `-Pstandalone=true` 排除会合并魔力池的 Ars ’n Spells。Aqua / Cat 定向构建通过。后续平台构建、哈希与部署结果在完成后追加。

未启动客户端，客户端 actionbar、移动手感、粒子和多人观察未人工验收。审计 D01–D04 的结果冷却/恢复期、生物资格、动作并发和执行角色差异保留；不以一次修复重写所有家族。

## 最终构建与部署

其他定向 Forge 用例：Goety 6、Mowzie 8、Iron 原生分池/外部账户 1，全部通过。BOMD / Aqua Forge 本批完成构建，没有再次运行完整旧能力矩阵。所有适用平台的最终二进制与源码包共 26 份检查通过，许可、上游素材排除和哈希见 [产物清单](cross-addon-audit-artifacts.json)。Cat 的 `verifyDistributions` 通过；Core/Iron 的实际 Forge JarJarSelector 同时验证最低 0.4.1 与整合包提供 0.5.5。

已串行更新“1.21.1-万法归一”的 Core 与六附属，以及“1.20.1-模组测试”已有的 Core / Iron / BOMD，共 10 个 JAR。每份复制后校验 SHA-256；旧包保存于各实例 `conflux-backups/20261013-052759/`，未触碰其他模组或用户配置。没有启动客户端；专服测试结束，不停止共享 Gradle daemon。

| 附属 | 新版本 | 修复提交 |
| --- | --- | --- |
| Irons-Nouveau | 0.17.19 | `65c4b6dab093b1df9cc7bfddca159535bc3b6740` |
| Goety-Nouveau | 0.1.0-dev.54 | `36933c9d21128dbe7976365de550f52be3e73877` |
| BOMD-Nouveau | 0.1.0-dev.11 | `2dcf599362ef4a6dc31bfafe7d137f725764c5d5` |
| Cataclysm-Nouveau | 0.1.0-dev.40 | `6f2c70a4a60ea37d94ea31b401b275a22bfac80e` |
| Aquamirae-Nouveau | 0.1.0-dev.13 | `e9c671cc3e1577bb4588e61ef5b917d257ab1347` |
| Mowzies-Nouveau | 0.1.0-dev.6 | `ea14231e3fa6c6cd0e8e77d1ab2298fd97570416` |
