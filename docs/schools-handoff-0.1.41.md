# Weighted schools handoff — 0.1.41

## Scope and defaults

- Worktree: `C:/Users/57991/Documents/MinecraftDev/Ars-Conflux`, branch `main`.
- Core source base: `ca21d6e31f44928841cdb5594fc594ea85497371`; this is a Core task, not an addon worker task.
- Targets: NeoForge 1.21.1 / Java 21 and Forge 1.20.1 / Java 17.
- Optional per-effect/per-payload weighted schools, data definitions/overrides, target rules and read-only author commands. Existing public damage API descriptors and `CastContext` remain compatible.
- Final user decision: **no preset schools**, no automatically classified glyphs. Core has an empty school catalog until another provider or a datapack defines entries.
- `[schools].damage_rules` defaults to `false`. No example datapack is deployed to the shared instance.
- Original Ars schools, damage sources, mitigation, resource payment and native ability behavior retain their existing responsibilities. Explicit native associations are metadata, not automatic classification.
- API and pack schemas: [schools.md](schools.md). Editable example: [school-pack](../examples/school-pack/README.md).

## Verification evidence

Commands below ran from the Core root (NeoForge) or `forge/` (Forge), with the corresponding JDK selected via JAVA_HOME.

```powershell
./gradlew.bat test --tests dev.arsconflux.SchoolDataTest runGameTestServer jar sourcesJar '-PgameTestNamespaces=ars_conflux_schools,ars_conflux_damage' --offline --console=plain --no-daemon '-Dorg.gradle.jvmargs=-Xmx512m'
./gradlew.bat runGameTestServer jarJar reobfJarJar sourcesJar verifyDistributions '-PgameTestNamespaces=ars_conflux_schools,ars_conflux_damage' -PgameTestHeap=900m --offline --console=plain --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx512m'
```

- NeoForge: 6 data/unit tests and 4 required GameTests passed (`build/schools-tests-neo-final.log`).
- Forge: the same 4 required GameTests passed; reobfuscation and distribution verification passed (`forge/build/schools-tests-forge.log`).
- One GameTest exercises actual datapack-loaded Harm weights, original Ars membership preservation, config-off behavior, native flat bonus plus weighted rules once, explicit empty scope/payload masking, actual Ars effect dispatch, delayed payload identity and cleanup.
- Three existing native damage cases cover prior damage events, cancellation and lifecycle regressions.
- Test-only school identities and rules stay in GameTest source/resources and are excluded from production JARs.
- After the user's final decision, removed six preliminary metadata definitions/constants and their translations. Replaced tests' references with test-local IDs; added an assertion that the default catalog is empty. Final targeted unit/build logs are `build/schools-no-presets-neo.log` and `forge/build/schools-no-presets-forge.log`; these changes do not alter the already-tested damage algorithm.
- No graphical client validation was performed. This version does not add a native Ars school UI or client school snapshot synchronization.

## Delivery

- Artifacts, SHA-256 hashes, successful logs, standalone example ZIPs for both data formats, deployment and final commit are recorded in `C:/Users/57991/Documents/MinecraftDev/releases/conflux-schools-20261030/manifest.json`.
- Shared deployment target: `C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一`. The old 0.1.40 JAR is retained under `.codex-backups/conflux-schools-20261030`.
- MIT and third-party notices retained. Forge includes only the existing permitted MixinExtras library, with declared refmap checked. No upstream mod classes, demo data, test classes or fixtures are packaged.
- No sibling addon was changed, and no CurseForge upload or remote push is part of this task.

## Next work

Only classify actual addon abilities once the desired taxonomy is decided. Each payload may have its own immutable weight profile; do not infer a whole spell's classification from its first effect. Preserve undeclared behavior, explicit empty masking and delayed profile inheritance. Follow existing verification evidence and run new tests only for newly changed behavior.
