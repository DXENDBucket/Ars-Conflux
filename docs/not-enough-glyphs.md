# Not Enough Glyphs

Conflux 0.1.12 在核心中维护 NEG 的可选适配。Iron、Goety、BOMD 只使用通用载体、上下文和资源接口，不分别安装 NEG 补丁。没有 NEG 时，这些可选 Mixin 不应用；发行包不包含 NEG、Sauce 或它们的素材。

## 衔接范围

| 机制 | Core 的处理 |
| --- | --- |
| 法术织册 / SpellBinder | 保留 `ThreadwiseSpellResolver` 的聚焦丝线逻辑；补上其覆写工厂中的 `ResolverExtension` 派生回调。 |
| 替换弹射物的碰撞解析器 | 将两种 `hasFocus` 查询交回原解析器；报价仍可创建织册的原生解析器，不让碰撞包装吞掉聚焦语义。 |
| 连锁、平面展开、过滤、转向、再次作用于自身或上下方 | 沿用 NEG 的目标选择与执行次序；衍生解析器保留来源，过滤失败不会执行后续提供者效果。 |
| 普通、弧线、追踪、溅射弹的再次发射 | 显式记录实际载体，并在子分支中计算可支付数量，再由 NEG 生成其原有散射。未配置提供者载荷时不减少数量。 |
| 回响轨迹 | 保留现有轨迹驱动、目标排除及每次触发的提供者支付；不会让替换实体本身成为自己的额外命中目标。 |
| 条件触发 / Contingency（1.21.1） | NEG 保存的解析器继续保留原施法者及来源；外部魔符仍在实际触发时执行自己的支付。 |

没有扩大魔符允许的组合，也没有把所有 NEG 内容改成触发收费。原生 Ars 的可选触发计费开关仍默认关闭；NEG 自身效果费用和条件触发次数仍由原模组决定。Core 不把被施法目标自动变成新的付款者。

## 生成载体的 API

`CarrierRegistry.bind(resolver, glyphId)` 给特定子解析器记录已注册的载体身份。标记随上下文克隆和解析器来源继承；后续子发射可以覆盖自己的标记。`CarrierRegistry.of(resolver)` 供预算读取，`of(projectile)` 供替换读取。实体为原版环绕球时仍优先识别为环绕载体。

这解决了 NEG 1.20.1 的实际差异：其再次发射使用合成的增强魔符作为配方首项，且不明确设置对应的 `castType`。仅检查配方或原 `castType` 无法可靠地区分弧线、追踪与普通弹射物。没有显式标记的第三方载体仍使用原有识别逻辑。

## 定向验证

开发依赖放在被 Git 忽略的目录中：

- NeoForge：`libs/neg/not_enough_glyphs-1.21.1-4.6.2.jar`。该发行包内含所需 Sauce。
- Forge：NEG 1.20.1-3.2.1.1，文件名设为 `forge/libs/neg/not-enough-glyphs-neg1201.jar`。

在对应构建目录执行：

```powershell
.\gradlew.bat :runGameTestServer -PwithNeg=true -PgameTestNamespaces=ars_conflux_neg
```

这些测试运行 NEG 本体，使用独立提供者夹具检查 Core 边界，不要求 Iron、Goety 或 BOMD。NeoForge 覆盖四组，Forge 覆盖三组；Forge 的旧 NEG 不含本轮使用的回响轨迹、条件触发与溅射再次发射内容。

默认构建不启用该开发依赖。NEO 的原有独立 Core 测试与 Forge 的原生计费测试用于确认 NEG 缺席时仍能启动并使用原生 Ars。

0.1.12 的本轮结果：NEG 环境 NeoForge 四组、Forge 三组全部通过；没有 NEG 的环境 NeoForge 四组、Forge 一组全部通过。两个发行包均完成构建与许可证、Java 版本、未捆绑上游内容的检查。

验证范围不等于所有法术组合都经过人工试玩。原生 Iron/Goety/BOMD 特效、多人客户端显示、织册每种丝线的数值及所有条件触发事件没有在这一批逐个复查；本轮主要覆盖来源、目标分支、支付与载体识别。
