# 魔符图标配置交付记录

2026-10-13；Conflux 0.1.25 / Iron & Nouveau 0.17.21。

## 范围与实现

- Conflux 实现提交：`b0670a87f01b704aa72838e12d6f3b3bc5bbf2d8`（`v0.1.25`）。
- Iron 实现提交：`cdf17b88b5cbdfea637af2d09eabbef969480edd`（`v0.17.21`）。
- 隔离工作区：`C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/glyph-visuals`；两仓库 `feat/glyph-visuals`，已快进本地主分支。
- 明确包含 Core API 与实现修改；完成时固定 Core 副本干净。其他附属不改代码，通过既有展示注册获得统一外框。
- 选项、API 与资源边界见 [图标文档](glyph-visuals.md)。默认原版分类边框、75% 中心、提供者图标；两个视觉开关同时关闭恢复旧外观。

## 验证证据

- Java 21 / NeoForge 21.1.250；Java 17 / Forge 47.4.0，分别使用实际本地 Ars 与 Iron 依赖。
- Iron 两端 `:runGameTestServer -PgameTestNamespaces=irons_nouveau_icons`，Neo 另加 `-Pstandalone=true`：各 1 项通过，遍历完整目录；无客户端类加载异常。
- Core `:test`：17 项通过，含 3 项新配色检查。
- 两端 `:runClient -PiconPreview=true`（Neo 加 `-Pstandalone=true`）：实际客户端截图检查分类、统一、自定义、原来源回退和淡化；Neo 同时加载 Cataclysm 3.33 / 附属 dev.48，核对原生三维长戟、变换与材质。预览类未进入发行包。
- Neo 截图：`Irons-Nouveau/run/icon-preview/screenshots/glyph-icons.png`；Forge 相应 `forge/run/icon-preview/`。主 Core `build/glyph-frame-preview/configured-glyph-icons-{neoforge,forge}.png` 保存副本。
- 发行与源码包均含 MIT 声明，不含原模组素材或测试类；Forge 重混淆完成，实际 JarSelector 范围验证通过。
- 尚未人工遍历完整整合包内所有页面和全部三维来源；没有为纯视觉修改重跑无关施法矩阵。

## 产物

| 文件（工作区仓库内） | SHA-256 |
| --- | --- |
| `Ars-Conflux/build/libs/ars-conflux-0.1.25-mc1.21.1-neoforge.jar` | `802cd5673919214d1fe6d45d4a26f4ace00a2729f5b6f447e343b20ecf14373f` |
| `Ars-Conflux/build/libs/ars-conflux-0.1.25-sources.jar` | `51fbae59169e8e964d1d4c6880cafa5d541aa5e3ada98edfe74434e9a45f5a9d` |
| `Ars-Conflux/forge/build/libs/ars-conflux-0.1.25-mc1.20.1-forge.jar` | `5c852d3a8c26a1f9b032a2e7a11ba75a2ed397682ab2fbdae2e4edbbcdd26a11` |
| `Ars-Conflux/forge/build/libs/ars-conflux-0.1.25-sources.jar` | `532df19aeedbc2265132859f8c91b44c28062099f98329cb9358e20c7bf33208` |
| `Irons-Nouveau/build/libs/irons-nouveau-0.17.21-mc1.21.1-neoforge.jar` | `83274de05b1171a948c55482067e54221c932e9d353efd34444df6fbe31ba4b2` |
| `Irons-Nouveau/build/libs/irons-nouveau-0.17.21-sources.jar` | `7a83018d51457725a653cb0bbebce32f1c84e5431d22a61f2569857ecc43b66f` |
| `Irons-Nouveau/forge/build/libs/irons-nouveau-0.17.21-mc1.20.1-forge.jar` | `9c8918c9841a4c9a2cf3d845409791f85cf0c9a55d6a2801dd03a21d2838225a` |
| `Irons-Nouveau/forge/build/libs/irons-nouveau-0.17.21-sources.jar` | `9b101e0cce00ccb26a9553893341be3591f7e14bfbc9b24f4de823b9f9301318` |

## 部署与后续

- 已串行更新 `1.21.1-万法归一/mods` 和 `1.20.1-模组测试/mods`，每个相关 modid 唯一，复制后 SHA-256 匹配；新增客户端配置取默认值。
- 旧 Core 0.1.24 / Iron 0.17.20 在各实例 `.codex-backups/glyph-visuals-20261013/`，未修改其他 JAR。
- 最终发行与源码包已复制各仓库主检出目录对应 `build/libs/`；本次客户端和服务端 JVM 全部结束。
- 未部署“万途归零”，未启动共享整合包完整客户端，未推送或发布。
- 下一个附属可使用 `GlyphVisuals` 提供新的原生中心；需要校准三维中心时调用 `GlyphIcon.placed`，复用 Core 绘制，不另加本地外框。
