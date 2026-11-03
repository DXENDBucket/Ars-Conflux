# NEG 调态分类：无展示提供者时的排序

## 复现环境

2026-11-04，Photon Test：Ars Nouveau 5.13.3、NEG 4.6.2（内嵌 Sauce 0.0.50.97）、Conflux 0.1.49、Ars Additions 21.3.0、Ars Controle 1.6.16、Spectra 0.1.2。没有 Iron / Goety 等登记 `GlyphPresentation` 的附属，Conflux 可选友军形态也未开启。

用户确认异常是调态分类的位置。Spectra 没有书页／分类注入；其飞行接管仅对原生 `EntityProjectileSpell` 精确类型生效。

## 原因与修复

Sauce 在 `GlyphFormatter.CATEGORIES` 追加子形态分类，其中有调态继承普通效果的排序索引。`GlyphFormatterOrderMixin` 已读取实际类别谓词，但 `GlyphPresentation.withinCategories` 在没有展示提供者时直接返回原生比较器，跳过了类别连续性整理。因此普通效果与调态可能按名称交错，标题也出现在一组内容中间。

0.1.50 删除该提前返回。类别排序不再依赖提供者登记；同一类别内，无登记提供者的魔符仍沿用原生比较器。没有改变魔符类型、制作、亮灭、施法或费用。

## 验证

新增 `UnregisteredGlyphCategoriesTest`，直接通过 `GlyphPresentation.withinCategories` 使用无登记提供者的魔符夹具，覆盖效果与子形态共享排序索引但名称交错的情况。修复前断言失败，实际顺序为调态／效果／调态／效果；修复后通过，效果连续、调态连续。

本次只对该问题执行单元回归、现有分类／分页测试及 NeoForge 构建与发行包检查；未启动客户端验证书页。

- `UnregisteredGlyphCategoriesTest` 1 项、`DisplayOrderTest` 2 项、`GlyphPageLayoutTest` 4 项，共 7 项通过。
- Java 21，离线 `:test`（仅上述三个类）、`:jar :sourcesJar` 成功。
- 发行包 CRC、版本、许可、Mixin 配置对应类完整性检查通过。
- 已替换 Photon Test 的 Core 0.1.49，Spectra 和其他模组未改。复制后 SHA-256 一致：`88911a22a097c141f586ef6fab4b4cc499f165ef96571d68e28f006916b69ed3`。
- 产物 `build/libs/ars-conflux-0.1.50-mc1.21.1-neoforge.jar`；本轮未构建 Forge，也未发布 CurseForge。

## 同期环绕问题

只读检查存档中的创造法术书，保存的零号槽是 `自身 → 环绕之球`，没有后续效果。实际 Ars 5.13.3 `EffectOrbit.onResolveEntity/onResolveBlock` 均在剩余编排为空时直接返回。需要在其后添加效果，如伤害；无需修改环绕生成逻辑或玩家存档。
