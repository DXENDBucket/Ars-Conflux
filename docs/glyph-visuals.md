# 魔符图标与客户端配置

Conflux 0.1.25；NeoForge 1.21.1 / Forge 1.20.1 使用相同选项。

```toml
# config/ars_conflux-client.toml
[glyph_icons]
frame_enabled = true
provider_icons = true
center_scale = 0.75
color_mode = "ARS_TYPE"
uniform_color = "#B79F69"
shape_color = "#B045E5"
augment_color = "#DC9613"
effect_color = "#1C919A"
```

- `frame_enabled`：引用 Ars 原版底板与分类边框。
- `provider_icons`：使用附属声明的替代中心。Iron 0.17.21 使用原生法术图标；关闭后使用该魔符原有物品模型。未声明替代中心的附属保持原模型。
- `center_scale`：有外框时的中心缩放，0.1–1；无外框恢复完整尺寸。
- `color_mode`：`ARS_TYPE` 引用原版分类边框像素，`UNIFORM` 使用统一颜色，`CUSTOM` 使用分类颜色。只重染彩色边缘，保留深浅和纸张。
- 颜色为六位十六进制 RGB，可有 `#` 前缀。

两个开关同时关闭恢复之前外观。仅为客户端配置，服务器无需同值；F3+T 从当前资源包重新读取素材。实际载入的新配置影响后续 GUI 绘制。

## 附属 API

现有 `GlyphPresentation.register(namespace, ...)` 提供者自动获得外框，无需复制渲染逻辑。也可只注册 `GlyphVisuals`。每个 namespace 最多注册一个中心来源提供者：

```java
// 二选一：原生 PNG 或原生物品模型（含三维 builtin/entity）
GlyphVisuals.register(MOD_ID, part -> GlyphIcon.texture(nativeSpellIcon(part)));
GlyphVisuals.register(MOD_ID, part -> GlyphIcon.item(nativeItem(part)).placed(1, 0, 0));
```

返回 `null` 保留现有魔符模型。声明 API 无客户端类，专用服务器可以注册。`placed(scale, x, y)` 是源模型校准；坐标以 16px 槽宽为单位，正 X 向右、正 Y 向下。比例再乘用户中心比例，须有限且在 `(0, 4]` 内。

## 渲染与资源边界

统一接管 GUI glyph item 绘制，包括背包、JEI 和 Ars 部件按钮；手持、地面与展示框保持原生外观。类别来自形态、增强、效果类，保留 Ars 无效编排淡化开关。

底板引用 `ars_nouveau:textures/item/glyph_template.png`，外围分类像素取自 `self` / `amplify` / `harm`，运行时组合成有界动态纹理缓存，不保存或打包原素材。缺失素材时回退。三维中心先缩放，再交回原物品渲染器，GUI 变换只应用一次；外框先画且不写深度，防止遮住模型后半部分。

Ars 的原淡化缓冲统一指定方块图集，无法直接沿用独立 PNG 和三维原生材质；Conflux 为这些中心保留纹理并单独缩放 alpha，非纹理类型保留原类型。

## 本次验证

两端各 1 项专用服务器 GameTest 遍历完整 Iron 目录，确认引用原生法术图标且未把客户端类带入服务器注册；Core 17 项单元测试通过（含 3 项新配色测试）。两端实际客户端预览通过：原版颜色、统一颜色、自定义颜色、旧版回退与淡化。NeoForge 同时检查了灾变原生三维长戟模型。

这是实际客户端绘制截图检查，尚未人工遍历完整整合包的所有 GUI 和所有原生三维物品。无需重跑无关施法测试。
