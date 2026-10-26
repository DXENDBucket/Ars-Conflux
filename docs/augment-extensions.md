# Native augment extensions (Conflux 0.1.32)

This batch adds explicit, optional repairs for selected upstream glyphs and signed numeric support in four Nouveau providers. No upstream code or assets are embedded. Existing recipes, glyph tiers and equipment mechanics are retained.

## Upstream compatibility

`config/ars_conflux-startup.toml` has a new default-on setting. Match both sides and restart to restore upstream eligibility/behavior:

```toml
[compatibility]
enable_augment_extensions = true
```

This switch governs the AAP/NEG/Controle extensions below, not each provider's own native augment support, nor the earlier AAP resolver fixes.

| Glyph | Extension | Semantics |
| --- | --- | --- |
| AAP Annihilate | AOE, Pierce, Sensitive | Reuses the existing native block-area, depth and shearing paths. No replacement mining engine. |
| NEG Flatten | AOE, Pierce | Reuses Ars' native area layout. Each simulated shovel interaction receives the corresponding block position and hit coordinates. |
| AAP Divine Smite | Extend Time, Duration Down | Adjusts the remaining native Shocked status duration; its 200-tick base/step are retained, with a minimum of one tick. Lightning lifetime is unchanged. |
| Controle Precise Delay | Weighted equivalents | One tier-II/III AAP Extend Time counts as 2/4 native instances. Delay remains 2^count ticks, saturating at the native 20-unit bound instead of wrapping an integer shift. This glyph is absent from the tested upstream Forge version. |

Installed classes are identified by their known implementation package, not guessed glyph namespaces. AAP 5/6 and NEG 3/4 have distinct class layouts, handled in loader-specific hooks where necessary. `AugmentEquivalence.refreshCompatibility()` runs after the additions, so supported higher-tier equivalents are also offered.

## Provider behavior

`AugmentSupport.withReductions(Set)` is an opt-in API that adds Dampen when Amplify is supported, and Duration Down when Extend Time is supported. It does not alter stats or enable extra modifier families. Callers must consume signed stats and clamp final outcomes.

- Mowzie 0.1.1: signed amplification reaches native damage, including mixed damage; output damage floors at zero. Existing channel, poison, geomancy and paw durations accept negative adjustments, with finite lower/upper bounds. Native status levels retain their minimum. Written positive amplification equivalents contribute weighted units to the existing surcharge; Dampen does not refund it.
- Aquamirae 0.1.1: matching negative augments affect status/sting levels, bite damage, effect/aura duration and the vortex health threshold. Native minimum levels are respected. Existing positive amplification equivalents contribute weighted units to the surcharge; negative modifiers do not refund it.
- Cataclysm 0.1.2: Wither Smoke and Scylla Lightning Field support AOE. Starting radius is `clamp(3 + AOE, 0.5, 16)` blocks. Native synced radii drive visuals and collision. Shrink rates still end at zero for smoke and 0.5 for the lightning field; wait times and damage remain unchanged.
- BOMD 0.1.1: Gauntlet Laser active output is `clamp(60 + 20 * duration, 1, 1200)` ticks, plus its existing 25-tick charge-up. Native aim lag, per-active-second payment and resource-exhaustion cleanup are retained. Existing client leases are refreshed/closed by the actual session, so output does not use a separate fixed expiry.

These provider builds require Conflux 0.1.32. AAP, NEG and Controle are optional and providers do not depend on one another. No new summon-size growth, disabled NEG glyph or generic expansion of body-animation geometry is included.

## Validation entrypoints

`ars_conflux_augment_extensions` exercises actual upstream block interactions, native lightning status duration and the precise-delay scheduler. The NeoForge-only `ars_conflux_augment_providers` namespace loads production provider JARs from ignored `libs/augment-extension-providers/` with `-PwithAugmentProviders=true`, alongside their real native mods. It checks actual Mowzie damage/lifetime, Aquamirae poison level/lifetime, Cataclysm synced radius/shrink rate and BOMD resource exhaustion, extended lifetime and per-second fees.

Presence checks use `-PwithAdamsArsPlus=true -PwithNeg=true -PwithControleAdditions=true`. The provider fixture uses the shared test instance's actual native versions. Normal runs omit the fixture. Server validation does not establish visual behavior in a client.

## Validation record — 2026-10-27

- NeoForge: all four required tests passed in the upstream + production-provider fixture. Runtime used Ars 5.13.3, AAP 6.0.5, NEG 4.6.2, Controle 1.6.16, Mowzie 1.8.2, Aquamirae 7.2.13 / Fragmentum 5.4.0, BOMD 1.3.3 and Cataclysm 3.33. Log: `build/augment-extension-neoforge-test.log`.
- Forge: all three registered tests passed with Ars 4.12.7 / AAP 5.0.3 / NEG 3.2.1.1. Two exercise upstream behavior; the Precise Delay case exits because this upstream Forge version has no such glyph. Log: `forge/build/augment-extension-forge-test.log`.
- Minimal NeoForge without AAP, NEG, Controle or the Nouveau providers started and passed the absent-integration test. Log: `build/augment-extension-absent-test.log`.
- All five projects built binary and source distributions on both loaders. Cataclysm's `verifyDistributions` passed on both loaders.
- Forge provider behavior was compiled, not separately replayed in a full coexistence GameTest. No client visual session was run for this batch.

The laser lifecycle test exposed a same-tick startup boundary: counting driver invocations could charge an extra active second before the world-time expiry. Its phase now uses elapsed world ticks, matching session expiry and retaining the 25-tick charge-up. The test verifies no fifth fee for an 80-tick active output, and depletion before a second active payment closes the session.
