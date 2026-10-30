# Conflux 流派示例数据包

复制本目录到世界的 datapacks，或使用交接目录提供的 ZIP。根 pack.mcmeta 面向 Minecraft 1.21.1（pack_format 48）；Minecraft 1.20.1 请将 pack_format 改为 15，或直接用对应 ZIP。

执行 /reload 后，用 /conflux schools glyph ars_nouveau:glyph_harm 查看权重。在 ars_conflux-server.toml 设置 [schools] damage_rules = true 才启用克制倍率；默认关闭。具体 JSON 格式与 API 见 ../../docs/schools.md。

示例只改 Harm：辉耀 70%＋凋亡 30%，辉耀对僵尸倍率 1.5，得到总体倍率 1.35。你可以重命名文件／命名空间并修改定义、权重和规则，建立自己的完整分类。此示例不会自动装入测试整合包，也不会附带在发行 JAR 中。
