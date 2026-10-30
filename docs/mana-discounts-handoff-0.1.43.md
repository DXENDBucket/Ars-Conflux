# 节魔戒指百分比报价交接

## 范围与实现

- Core 基线：`b8cb7c9974c896844b72c80241968c46ba0f99a1`；工作树 `C:/Users/57991/Documents/MinecraftDev/Ars-Conflux`。
- 目标：NeoForge 1.21.1 / Java 21；Forge 1.20.1 / Java 17，Core 0.1.43。
- 原生节魔之戒转换默认开启，初级 10%、高级 20%；可分别配置，合计封顶 100%，正费用向上取整。开关关闭恢复原生 10／20 固定减耗。
- ManaUtil 原生入口保留费用事件与其他固定减耗；ResourceAccount.quoteCost 在报价构造时折价，所有预算与扣费读取相同最终值。灵魂能量不参与。
- `ResourceCharge` 保留旧二参数构造器和访问器，新增原价与 exact 工厂。原生 Ars 已完成减耗的费用不再报价。
- Iron 0.17.34：首次预算读取最终价，Iron Mana 账户也接入。
- Mowzie 0.1.2：地潜维护附加费依据 originalAmount，避免在折后价上推导原始费用。
- 用户未回复比例偏好时采用已说明的 10%／20% 默认值；数值均配置化。

## 验证

- Core Neo：`:test --tests dev.arsconflux.ResourceTransactionsTest :runGameTestServer :jar :sourcesJar -PgameTestNamespaces=ars_conflux_discounts,ars_conflux_billing -PgameTestHeap=900m`，6 项资源单元测试、2 项 GameTest 通过。
- Core Forge：`:runGameTestServer :jarJar :reobfJarJar :sourcesJar :verifyDistributions`，相同两个 namespace，2 项 GameTest 通过。
- 两端均验证真实 Curios 戒指、原生全价／触发费用、关闭恢复、100% 折价、散射数量、嵌套预留、报价冻结和重复触发。Forge 测试使用真实 Mana capability，不套用 Neo 的 wrapped-caster 测试替身。
- Forge 最终包的戒指 tooltip 映射确认是 `m_7373_`，Component.translatable 也具有 SRG 映射。
- 旧 ResourceCharge 公共构造器／方法描述符保留；发行包排除 GameTest 类并含许可证。
- 客户端 tooltip 外观未人工验证；没有启动 GUI。

## 交付

- 原始日志在各仓库 `build/discount-*.log` 或 `forge/build/discount-*.log`。
- 最终包、哈希、日志和提交见 `../releases/conflux-discount-rings-20261030/manifest.json`。
- 本批不推送或上传 CurseForge。共享万法归一由当前协调者串行部署；不更改万途归零。
