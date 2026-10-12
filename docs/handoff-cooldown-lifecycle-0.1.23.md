# 并行与冷却生命周期交接（2026-10-13）

Core 0.1.23，运行代码 `21fb59af5ad4c1be96ed86e67b7dfc8fbc8bc59d`。工作树 `parallel-workspaces/cross-addon-core/Ars-Conflux`，本批独立分支 `fix/cooldown-lifecycle`；附属 Core 副本固定此提交、受版本控制文件只读。

## 用户明确的边界

非体术效果允许并行，包括所有吐息和 Iron 冲锋。只有体术模拟的实体动作使用独占/替换；吟唱准备、主动冷却和资源支付照常。共享 channel / ACTIVITY 本已非独占，本批修正 Iron 绑定工具在它们存在时误判忙碌的问题。

实体命中续接优先参考原生伤害成功，减少穿透/持续接触导致的密集后续触发；方块和无伤害载体单独处理。海灵回旋武器保持伤害成功续接，深渊碎晶本批保持既有碰撞续接。其他弹体家族没有在冷却修复中批量更改。

## 冷却职责

- `PersistentCooldowns` 是提供者主动登记的持久记录 API，读取旧 NBT 字段，采用主世界时钟，多个来源只延长期限。
- `commit` 在同步主动释放的全部分支结束后提交成功结果；无成功派发不提交。后续独立命中/结束恢复即时提交，源策略关闭冷却时不写入。
- Core LOWEST Clone 钩子复制已登记记录。`[addon_cooldowns].preserve_on_death` 默认 true；非死亡克隆始终保留。Iron / Goety 原生存储保持原生规则。
- BOMD / 灾变 / 海灵 / Mowzie 接入公共存储，保留各自时长、创造、资格和发射/结果/恢复期语义。BOMD 启动时主动初始化注册，避免首次施法前重生漏复制。
- 海灵持续光环不再动态延长施法者的新释放阻塞时间；授予冷却延后到本次同步释放结束，结束恢复期保留。不同友军会话仍独立支付和清理。

## 验证与限制

Core NeoForge / Forge 各 6 项：存量字段、只延长期限、复制隔离、死亡保留开关、非死亡复制、同步分支结果提交/失败/源禁用，并保留执行锚点与替换回归。复制验证调用公共 API，未人工执行客户端死亡/末地返回。

Iron NeoForge / Forge 各 1 项：绑定工具的体术可替换、两个 shared channel 并行、ACTIVITY 不阻塞。海灵两端各 3 项：实际光环的独立友军宿主、同次嵌套资格与结果提交、到期后不由持续状态额外阻塞、恢复期与复制。其余改动附属完成适用平台构建，未重复完整旧能力矩阵。

二进制与源码包共 22 份许可/上游素材/依赖/测试代码排除检查通过，清单 [产物哈希](cooldown-lifecycle-artifacts.json)。Cat `verifyDistributions` 通过。所有测试服务结束；未启动客户端，没有人工验收多条原生吐息的视觉/HUD、实际移动手感或多人观察。

## 版本控制与部署

本地提交并合回主分支、建立版本标签；没有推送或公开发布。已串行更新“1.21.1-万法归一”的 Core 与五个本批改变的附属，Goety 原包保留；Forge 测试实例只更新已安装的 Core / Iron / BOMD。共 9 个 JAR，复制后哈希核验。旧包备份目录 `conflux-backups/20261013-061112/`，未改用户配置。

| 附属 | 版本 | 提交 |
| --- | --- | --- |
| Irons-Nouveau | 0.17.20 | `4b1be102292cd1d2d0bd35ecf1cbc863eee3ecd7` |
| BOMD-Nouveau | 0.1.0-dev.12 | `3ce6a4a54755eadd5a6eb0be43caec366ca6e15c` |
| Cataclysm-Nouveau | 0.1.0-dev.41 | `16417780fa55625dfdfc3c62d3685c7eabc8bb6e` |
| Aquamirae-Nouveau | 0.1.0-dev.14 | `0357e9051dd7997e7176e8d586a5fd1af76fdaaf` |
| Mowzies-Nouveau | 0.1.0-dev.7 | `a583e0bf49d6a39a44d2e7e59729c72d3c579a1a` |
