# 0.1.30：GUI 外框方向光修复

用户在 Cataclysm & Nouveau 的 1.20.1 移植与 1.21.1 预览中指出部分魔符偏暗。

原来的 `entityTranslucentEmissive` 着色器仍在顶点阶段根据 normal 和两盏物品方向光执行 `minecraft_mix_light`。不同原生模型的 `gui_light` 会切换平面／立体光照，导致完全相同的二维外框出现不同亮度。仅设置满亮 lightmap 无法避免这一点。

两平台的 `GlyphIconPlatform.draw` 改为满亮、不参与方向光、也不写入深度的 `textSeeThrough` 平面绘制。去除该顶点格式不需要的 overlay 和 normal 数据。中心三维物品继续使用原生渲染和光照，Ars 未学会／无效魔符的透明度规则与原有配置保持不变。

这是 Core 的中立显示修复，没有引入任何灾变特有 API、资源或判断。原生三维物品身份和深渊标记图集遗漏分别在灾变附属 0.1.1 修复。

验证：Java 17 Forge、Java 21 NeoForge 构建；与灾变 0.1.1 一起在两个真实客户端分别渲染全部 102 个 GUI 图标和 12 个三维物品图标。仅修改客户端绘制，不重跑与此次修改无关的服务端全矩阵。没有宣称逐一实测全部法术粒子和动作。

最初基线 `b46be94b17799573b825a8e4a11870fa3dc857d6`，独立工作树 `parallel-workspaces/conflux-glyph-render-20261027/Ars-Conflux`。灾变移植的固定 Core 副本保留只读。
