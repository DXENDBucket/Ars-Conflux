# Adam's Ars Plus

Conflux 0.1.31 supports the published Adam's Ars Plus 6.0.5 on NeoForge 1.21.1 and 5.0.3 on Forge 1.20.1 as optional integrations. No AAP code, textures or JARs are shipped. Iron & Nouveau 0.17.28 and Goety & Nouveau 0.1.1 consume the shared augment API and require Conflux 0.1.31.

## Equivalent augments

`AugmentEquivalence.register(augmentId, baseId, units)` declares a startup-time, one-step semantic equivalent. II contributes 2 units and III contributes 4 units for Amplify, Dampen, AOE, Accelerate, Extend Time and Duration Down. Lesser AOE contributes -1 AOE. Native glyphs still contribute one unit. Equivalents do not expand into multiple recipe slots, bypass Ars limits, change glyph tiers or alter costs.

`refreshCompatibility()` applies registered equivalents to Ars eligibility sets after provider registration. Core calls it at load completion; a late registrant may call it explicitly.

`sum` is for rules such as native spell levels that cannot use `SpellStats` directly. `matches` supports exclusions and presence checks; `supports` and `expand` support eligibility sets. Numeric effects should keep reading native `SpellStats`: calling `sum` again on an already modified stat would double the benefit. Registration rejects cycles, conflicting definitions and non-finite/zero weights.

The optional adapter locates the actual installed AAP glyph classes and registers their real IDs. This matters because AAP 5 uses `adamsarsplus` IDs, while AAP 6's published glyphs use `ars_nouveau` IDs. It does not claim another mod's same-named glyph. Carrier profiles expand their supported base augments dynamically; load-complete setup updates Ars palette sets after provider registration. Unsupported modifier families remain unsupported.

Iron translates equivalent Amplify/Dampen units into its existing native level policy. No native maximum is introduced. Goety's legacy Soul/Magic Bolt potency and Magic Bolt duration use the same weights; effects and other projectile adapters already read `SpellStats`. Goety's life-absorption/Amplify exclusion also recognizes equivalent augments and reports the actual incompatible slot in the Ars editor.

## Domain and Swap Target

- AAP's native domain entity, interval, radius, lifetime, shell, particles and targeting remain in AAP. Provider effects keep their existing success-based billing and cooldowns. A repeating domain is not a free bypass of either rule.
- A sliced domain continuation is reset to index zero. AAP 6 otherwise carries the old parent cursor into its per-hit `makeChildContext` call, slicing the remaining recipe twice. Parent cancellation remains local to the parent.
- AAP 5 constructs a fresh resolver for the domain. Conflux retains source resolver extensions there. AAP 6 already uses `getNewResolver` and reaches the standard Core derivation hook.
- Swap Target's fresh resolver retains source extensions and the entrypoint's explicit preparation/cooldown policy. Its newly constructed native context keeps AAP's exchanged living caster and native wrapped-caster behavior. Previous Conflux damage ownership, payment-account overrides and incoming direction are not forced onto the new caster.
- Selecting a target still does not bypass provider self-only or alliance restrictions. Hex/NEG integrations use the shared resolver-extension API rather than provider-specific AAP code.

## Costs and boundaries

Spell Efficiency continues to affect the native Ars `SpellCostCalcEvent` bill. It does not discount Iron's trigger fee, Goety soul energy, or other provider-specific charges, including provider fees paid from an Ars Mana pool. AAP's own augment prices and exhaustion remain unchanged. Conflux's optional native-Ars trigger billing retains its separate configuration and default.

This is not a reimplementation of AAP bosses, Ten Shadows progression, summon ownership or automatic-casting equipment. Those retain their native behavior. The integration does not promise that all special equipment reproduces player-active chanting.

## Reproducing verification

Development-only dependencies live in ignored `libs/adams-ars-plus/` (NeoForge) and `forge/libs/adams-ars-plus-adams1201.jar` (Forge). Enable with `-PwithAdamsArsPlus=true`. The published runtime has no AAP dependency.

The `ars_conflux_adams` GameTest namespace exercises actual AAP augments, native domain continuations with finite provider resources, explicit caster exchange, source resolver extensions and entrypoint policy. `ars_conflux_adams_providers` additionally loads production Iron/Goety JARs from ignored `libs/adams-ars-plus-providers/` using `-PwithAdamsProviders=true`, and checks Iron level conversion, projectile validation, Goety absorption eligibility and the native Magic Bolt's potency.

Native gameplay uses the original client renderers. Server GameTests do not establish that every AAP particle, boss or optional-addon combination has been visually reviewed.

### Verified 2026-10-27

- NeoForge: Ars 5.13.1, AAP 6.0.5, native Iron 1.21.1-3.16.3, native Goety 3.2.1. Four actual-mod GameTests pass, including the production Iron/Goety JARs together. Magic Bolt receives +4 native potency and +4 native duration from the tier-III augments.
- NeoForge without AAP or providers: one standalone startup/casting GameTest passes.
- Forge: Ars 4.12.7 and AAP 5.0.3. Three actual-mod GameTests pass. Core, Iron and Goety production distributions compile and package for both loaders; Forge provider coexistence was not rerun in this batch.
- No client visual session was launched. No upstream code/media are included in the six addon/core production JARs, and each retains its license.

Logs are in the ignored Core `build/adams-neoforge-test.log`, `build/adams-absent-test.log`, and `forge/build/adams-forge-test.log`; provider build logs use `build/adams-neoforge-build.log` and `forge/build/adams-forge-build.log` in each provider checkout.
