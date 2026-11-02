# NEG 形态调态（0.1.48）

在安装 Not Enough Glyphs 时，Conflux 可以为自己的“周围友军”和 Elemental 的“卡利亚圆阵”提供调态魔符。沿用 NEG 的 `IPropagator.copyResolver` 子上下文、剩余编排和父分支取消规则，不另做一套协议或多次交互。

- `ars_conflux:glyph_propagate_nearby_allies`：周围友军调态。需要已启用 `enable_nearby_allies`，默认三级、100 Mana；扩散改变原形态的搜索范围。
- `ars_conflux:glyph_propagate_carian_phalanx`：卡利亚圆阵调态。需要原生圆阵实现及其注册条件，默认三级、100 Mana；分裂交给原生圆阵处理，包括原生五发限制。当前 Forge 1.20.1 的 Elemental 无该实现，因而不注册。

两种都是子形态，不能作为编排的起始形态。例如：`弹射物 → 周围友军调态 → 治疗`；触发点只负责触发调态，友军搜索仍以原施法者为中心，圆阵也环绕原施法者，与 NEG 自身调态一致。没有把命中生物变成新的施法者或付款者。

`ars_conflux-startup.toml` 的 `[optional_content]` 新增 `enable_neg_form_propagators = true`。这是派生内容的总开关；对应原形态的开关仍默认关闭，因而不会无条件改变标准内容。启用状态需两端一致并重启。

获得方式为 Ars 抄写台：对应的形态魔符 ×1、源质精华 ×1（友军使用召唤精华，圆阵使用操纵精华）、魔源宝石 ×1，经验 160。配方有“魔符确实已注册”的条件，缺少 NEG、禁用开关、缺少圆阵时均不会留下无输出配方。

NeoForge 使用 NEG 所带 Sauce 的传播接口；Forge 使用 NEG 自己的旧接口。接口仅为编译依赖，发行包不包含 NEG／Sauce 的类或素材，运行时也不要求安装它们。原生圆阵的实体、渲染和载体身份仍由 Elemental 与既有 Conflux 适配负责。

实现：`PropagateMethod` 保存共享委派逻辑；两端 `NegFormGlyphs` 只链接对应的原生接口。`OptionalGlyphs` 在检查模组存在后才进入可选实现。原有施法归属、来源扩展和资源账户通过 Core 的子上下文继承。

客户端外观未人工验证。具体构建、GameTest、资源检查和产物哈希见 `neg-form-propagators-build.json`。本次没有部署共享实例、推送或上传 CurseForge。
