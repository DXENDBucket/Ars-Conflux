# Percentage spell-power handoff — 0.1.42

## Scope

- Core: `C:/Users/57991/Documents/MinecraftDev/Ars-Conflux`, branch `main`, source base `3217891e07747f6a50516016945b86280f6556de`.
- Iron: adjacent `Irons-Nouveau`, base `09fdd12e19d75ba64543bfe0a15d4181aab73ef0`, version 0.17.33.
- Goety: adjacent `Goety-Nouveau`, base `44b9d97f471613bb92b3943cab9183648c56a042`, version 0.1.6.
- Both addons now require Core 0.1.42. Both Minecraft/loader targets are maintained: NeoForge 1.21.1 / Java 21 and Forge 1.20.1 / Java 17.
- All edits are sequential in the existing main worktrees; no agents or parallel test JVMs were launched.

## Implementation

- `ars_conflux:confluence_spell_power` is the percentage general attribute, default 0. It applies to undeclared spells too.
- Startup `schools.power_attributes` registers optional per-school percentage attributes at `ars_conflux:school_power/<namespace>/<path>`. Empty by default; no preset schools or gameplay progression.
- Add general percentage and the weighted school percentages, then clamp the combined multiplier at zero. Individual attributes accept negative values down to -1024. Positive values up to 1024. Numeric zero is handled before multiplying target rules, avoiding zero-times-overflow NaNs.
- `SpellPower` owns the formula, raw-native path, immutable payload profiles and exception-safe guard/scopes. Native providers do not implement their own percentage formula.
- Ars Pre and raw native hurt are mutually exclusive calculation boundaries. Existing NativeSpellDamage API still adds the Ars flat bonus once and dispatches events; old public signatures and CastContext record components remain unchanged.
- Profiles retain the original caster plus executor/damage-owner roles. An unrelated attacker inside a spell callback cannot borrow its caster's power. DamageSource objects are not globally cached/mutated as an accounting shortcut.
- Ordinary nonliving spell payloads capture the profile on spawn; ServerLevel tick hooks preserve it for delayed native damage and emitted children. Ordinary summoned living creatures are not implicitly given a permanent melee multiplier.
- Shared execution callbacks preserve the per-effect school snapshot. Replacement projectile forms capture their own glyph declaration, separate from subsequent effect glyphs.
- Iron motion and native-self leases preserve school snapshots. Existing paid area/emitter leases also store school weights and restore runtime profiles, including living wisps while leaving ordinary summon melee native. Account selection and billing are unchanged.
- Goety Bolting's separate timed task captures/restores a profile. Healing, duration, potency, resources, native stats and ordinary upstream casting retain their original responsibilities. Goety Killing feedback explicitly opts out of percentage scaling; legitimate self-targeted spell damage uses the same formula as other targets.
- General and school attributes are synchronized to all entity types with attribute suppliers. Dynamic school names have a localized client fallback; resource packs can provide exact names. Neo uses a percentage attribute formatter; Forge adjusts only these attributes in ordinary item tooltips.

## Verification

- Core NeoForge: six school-data unit tests and five GameTests passed. Logs: `build/power-tests-neo-complete.log`. New coverage: general power without schools, weighted percentages, <-100% plus positive offsets, native Ars, old common damage bridge, Ars-event/native-hurt double-scaling prevention, delayed payloads/emitters, session restoration, ordinary magic isolation and cleanup.
- Core Forge: five equivalent GameTests passed (`forge/build/power-tests-forge.log`); later final combined build/test evidence is in Iron's `forge/build/power-final-forge.log`.
- A world-wide cleanup in the old damage tests interfered with the school test's delayed attachment when run concurrently. School and power tests now have separate batches; this is fixture isolation, not weakening assertions.
- Actual NeoForge coexistence: one focused Goety test exercises native Goety Soul Bolt, native Iron Firebolt and Goety Thunderbolt through Ars, checking +50% exactly once. Iron 0.17.33, Goety 0.1.6, Ars 'n Spells 3.3.2 and NEG were present. `Goety-Nouveau/build/power-coexistence-neo.log` contains the successful one-test summary.
- The same Gradle invocation accidentally selected the Core subproject's unqualified runGameTestServer too; that extra process had no matching Goety test functions. It is not counted as a successful test. Future addon commands must use `:runGameTestServer` to target the root explicitly.
- Native client visuals and attribute tooltip appearance were not manually inspected in a graphical client. Forge client Mixin selectors/refmap are checked in the final distribution audit. Forge addon runtime coexistence was not rerun; addon source/build compatibility is verified separately.
- Final NeoForge verification with Ars ’n Spells 3.4.0 and the new source-role/self-damage guards: `Goety-Nouveau/build/power-final-neo.log` records two separate one-test passes (the root coexistence test and the explicitly selected Core power test).
- Later final targeted tests and exact source/commit/hash information are recorded in the release manifest alongside the logs. Do not interpret a Gradle success alone as a test success; require the explicit required-tests-passed summary.

## Delivery

Final distribution hashes, source commits, commands/results and deployment are recorded under `C:/Users/57991/Documents/MinecraftDev/releases/conflux-spell-power-20261030/manifest.json`.

No remote push or CurseForge upload is authorized by this task. Shared instance deployment is coordinated sequentially, after checking that 万法归一 is closed. No test datapack or test school registration is deployed. No sibling addon outside Iron/Goety was modified; existing common entrypoints gain the Core functionality without recompiling those addons.

## Continue

Read [spell-power.md](spell-power.md) and the shared development workflow. Keep native stats and costs separate from the common percentage. New independently scheduled native damage needs an explicit captured profile or an existing shared execution/payload entrypoint. Do not classify an entire spell by its first effect, apply the percentage twice, or let an executor replace the source caster for power lookup.
