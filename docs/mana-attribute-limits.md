# Native Ars mana attribute ceilings (0.1.46)

Ars attributes can clamp a larger calculated pool before events see its intended value. The reviewed Ars 5 build limits maximum mana to 10,000 and mana regeneration to 2,000. Conflux can relax those numeric ceilings at common setup on both NeoForge and Forge.

`config/ars_conflux-startup.toml`:

```toml
[mana_attribute_limits]
enabled = true
maximum_mana = 1000000
maximum_regeneration = 100000
```

- These values are ceilings, not base mana, regeneration grants or extra pools. They apply to native player attributes and explicitly configured non-player pools alike.
- An existing higher native/modded ceiling is never lowered. Values below the existing native ceiling therefore do not impose a new limit.
- `enabled = false` preserves native/other-mod bounds. Changing any option requires restart; use matching client/server settings.
- Integer options support 0 through 2,147,483,647. Native account/storage and event semantics still apply; this does not make arbitrary amounts infinite or bypass costs.
- The implementation contains no entity types, boss values, AI, soul storage or modpack progression rules.

Shared arithmetic and the RangedAttribute accessor live in common sources. Each platform contributes its native Ars attribute handles at common setup. Forge's COMMON configuration is already available at that point; no new registry-time file reader is required.

Targeted integration validation is recorded with Abyss Trials dev.21: native startup ceilings, disabling the option on independent attributes, preserving a higher existing ceiling, and a 50,000 mana / 2,500-per-second real native pool. Forge packaging also checks its declared refmap.
