# 节魔之戒百分比减耗

Conflux 0.1.43 默认将 Ars 原生初级／高级节魔之戒的固定 10／20 Mana 减耗转换为 10%／20%。只转换这两种原生戒指；其他装备的原生固定减耗继续由 Ars 处理。

世界的 `serverconfig/ars_conflux-server.toml`：

```toml
[mana_discounts]
percentage_discount_rings = true
lesser_ring_reduction = 0.10
greater_ring_reduction = 0.20
```

比例范围为 0–1。同一施法者装备的戒指比例相加，上限 100%；不足一个 Mana 的正费用向上取整。100% 可将费用降至零。关闭转换后，两种戒指恢复原生固定减耗，附属触发费用也不再获得此百分比。

原生 Ars 的常规预付与可选触发计费都使用原生费用入口，先保留其他固定减耗，再应用戒指比例。附属 Mana 触发费用通过共享资源报价处理；Iron 自有 Mana 池也接入。Goety 灵魂能量保留本体规则。创造模式和非生物设备原有规则不变。原生 Iron 法术书自身施法不在此次转换范围内。

## 资源报价 API

`ResourceAccount.quoteCost(raw)` 是无副作用的价格转换，默认返回原价。`ArsManaAccount` 对生物施法者使用戒指比例。账户的 `canSpend` 与 `spend` 接受最终费用，不能再次减耗。

`new ResourceCharge(account, raw)` 固定本次报价；`amount()` 是折后费用，`originalAmount()` 是提供者原价。预估、散射数量、嵌套预留与扣费都使用同一份报价。报价后换装备不会改变已预留的费用；后续新报价读取新装备。

`ResourceCharge.exact(account, finalAmount)` 用于已经完成折价的费用，防止重复处理。根据原价计算另一次维护费／附加费时读取 `originalAmount()`，再独立报价。旧的二参数构造器及原有访问器仍保留。

Iron 0.17.34 对首次施法预算和 Iron Mana 账户接入报价；Mowzie 0.1.2 修正地潜维护附加费从原价推导。其他通过 Core ArsManaAccount 建立 ResourceCharge 的附属无需逐个修改。
