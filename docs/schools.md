# 可选、多权重流派（0.1.42）

这一层扩展 Ars 的流派信息：Ars 原生 `SpellSchool` 仍负责原来的成员、子流派和本体机制；Conflux 提供可选的带权归属、显示元数据和目标伤害规则。两者可以显式关联。关联不会推断权重或改写原生成员，也不会替换原生 DamageSource。

没有声明的魔符／伤害保持未声明；空配置不会自动落入某个默认流派。现有附属不必修改就能继续运行。本版不自动给 Iron、Goety 或其他附属分配权重；它们后续可以逐批声明。

## 默认状态

Core 不内置任何预设流派、魔符归属或目标克制规则。流派名称、颜色、归属和倍率均由附属或数据包作者提供；下面的自定义流派仅用于独立示例，不会随模组自动启用。

## 数据包布局

所有文件使用标准数据包优先级，并支持 `/reload`。

```text
data/<namespace>/ars_conflux/schools/<school>.json
data/<glyph_namespace>/ars_conflux/glyph_schools/<glyph_path>.json
data/<namespace>/ars_conflux/school_damage/<rule>.json
```

文件路径决定资源 ID。魔符使用 `AbstractSpellPart.getRegistryName()` 的 ID，不是物品显示名称。普通 Harm 的 ID 为 `ars_nouveau:glyph_harm`。适配者也可将有独立语义的子能力注册为独立声明 ID，再显式绑定其权重。

### 1. 自定义流派

`data/example/ars_conflux/schools/radiance.json`：

```json
{
  "name": "辉耀",
  "color": "#F2BC53",
  "ars_schools": ["fire"]
}
```

这会定义 `example:radiance`。`name` 是可直接使用的字面名称；可选 `translation_key` 让客户端资源包提供翻译。颜色可用 RGB 整数或六位十六进制字符串。`ars_schools` 是可选的原生 Ars School ID 元数据（例如 `fire`）；它本身不改变原生魔符分类或加成。

### 2. 分配权重

`data/ars_nouveau/ars_conflux/glyph_schools/glyph_harm.json`：

```json
{
  "weights": {
    "example:radiance": 7,
    "example:decay": 3
  }
}
```

归一化为 70% 辉耀、30% 凋亡。权重不要求预先加到 1 或 100；正数有效，0 被忽略，负数与非有限数值被拒绝。分量数量不限。

数据包优先于附属的代码声明。可用下面的完整文件显式清空某魔符的 Conflux 归属：

```json
{"weights": {}}
```

删除覆盖文件并 `/reload` 后恢复代码声明；没有代码声明则恢复未声明。定义或引用未知的命名空间不要求相应模组安装。缺少定义元数据的 ID 保留权重，其未匹配的伤害倍率为 1。

### 3. 目标克制规则

`data/example/ars_conflux/school_damage/radiance_zombie.json`：

```json
{
  "school": "example:radiance",
  "target_entity": "minecraft:zombie",
  "priority": 10,
  "multiplier": 1.5
}
```

也可使用 `target_tag` 指定实体类型标签。两个条件都出现时取交集；都省略时匹配所有目标。同一流派采用最高 `priority` 的匹配规则，同优先级按完整规则 ID 的字典序决定。每个流派未匹配时为 1。

各流派倍率按权重求平均：70% 辉耀（×1.5）＋30% 凋亡（×1）得到 ×1.35。只有流派归属而没有有效规则时，倍率为 1。规则倍率允许 0，要求有限且非负；这一层不会改变伤害类型或绕过原生护甲／免疫。

## 启用伤害规则

`ars_conflux-server.toml`：

```toml
[schools]
damage_rules = false
```

默认关闭，保留现有平衡。设为 `true` 后才应用数据包的目标倍率。NeoForge 使用实例 `config`，Forge 使用世界的 `serverconfig`；新世界默认值可放进 `defaultconfigs`。

既有原生基准、增强和百分比封顶先由提供者处理；Conflux 原生伤害桥接仍只加一次 Ars 通用平加值。公共 Ars 路径的流派倍率在 Pre 事件的 LOWEST 阶段应用；原生路径在最终 hurt 应用，二者互斥，然后进入原生护甲／抗性结算。已经拥有目标克制的原生法术，需要适配者或包作者决定是否再配置规则。提供者可为具体一次原生命中显式传入 `SchoolWeights.EMPTY` 保留本体处理。

0.1.42 新增默认值为 0 的汇流百分比法强，以及可在启动配置中声明的流派百分比法强，详见 [法强属性](spell-power.md)。流派法强不自动注册；原有 Ars／Iron／Goety 加成继续遵守各自原生规则，未声明流派也能享受汇流法强。

## 查看与排错

需要权限等级 2：

```text
/conflux schools list
/conflux schools glyph ars_nouveau:glyph_harm
```

可以查看有效定义和有效权重。格式错误的资源会在服务端日志中警告并跳过，其余有效资源继续加载。一个重载快照整体替换旧数据。

这一版提供服务端数据、查询 API 和管理命令，不改写原生 Ars 流派 UI。自定义名称可用字面文本，不需要额外客户端资源；使用自定义翻译键时应同时提供资源包。

## 附属 API

```java
var radiance = ResourceLocation.tryParse("example:radiance");
var decay = ResourceLocation.tryParse("example:decay");
GlyphSchools.declare(myGlyph.getRegistryName(), SchoolWeights.of(Map.of(
    radiance, 7,
    decay, 3
)));
SchoolRegistry.associate(radiance, SpellSchools.ELEMENTAL_FIRE);
```

`SchoolRegistry.register(SchoolDefinition)` 可提供代码元数据默认值。数据包随后可以覆盖这些默认定义或归属。`SchoolWeights` 是不可变快照，提供 `shares()`、JSON 读写和 `multiplier(id -> value)` 加权平均。

普通 Ars `AbstractEffect` 的原生分发在现有 Core 分发钩子中建立独立流派作用域：后一效果未声明时会屏蔽前一效果的归属。触发计费开关不控制这个归属作用域，也不会因此多收 Mana。

原生弹体、范围效果和延迟子载荷应显式保存每个载荷的归属：

```java
var schools = GlyphSchools.get(myGlyph);
NativeSpellDamage.bind(projectile, context, schools);
NativeSpellDamage.bind(child, context, SchoolDamage.profile(parent));
NativeSpellDamage.during(context, schools, nativeAction);
NativeSpellDamage.hurt(context, schools, target, nativeSource, amount, target::hurt);
```

原有 bind/during/hurt 签名继续有效。原有 `bind(entity, context)` 会捕获当前效果作用域的归属；在普通分发外没有归属时为空。后续子载荷由提供者显式继承，不通过整条编排推断。资料附着只在运行时存在，卸载／停止时清理，与原有伤害上下文的持久化边界一致。

`NativeSpellDamage.hurt` 的显式权重覆盖该次命中的载荷归属；空权重可以屏蔽外层。原生弹体在分发作用域之外命中时，使用其绑定的快照。规则在命中时读取当前服务端数据；已生成载荷的权重快照保持不变。资源支付、友军处理、伤害归属、命中续接和持续效果生命周期仍由原有 API 负责。

## 可修改示例

仓库 `examples/school-pack/` 含完整示例：新增辉耀／凋亡流派，将 Harm 分为 7:3，并设置辉耀对僵尸 ×1.5。示例不会打包进模组 JAR；需要作者主动安装并开启伤害规则。发行交接目录提供两平台格式匹配的 ZIP。
