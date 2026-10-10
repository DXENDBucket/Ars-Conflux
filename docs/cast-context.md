# Unified cast context

`CastContext` supplements Ars' `SpellContext`. Native spell parameters and formulas remain provider-owned. All supported providers can use these same role definitions without depending on each other.

| Field | Meaning |
| --- | --- |
| `ars` | Current Ars context; nullable when restoring provider-owned persistent effects. |
| `caster` | Original source of permissions, levels and spell power. |
| `executor` | Living entity performing an action; nullable for a fixed point. |
| `damageOwner` | Entity credited for damage; selecting an executor never changes it implicitly. |
| `account` | Actual payment account. An entity hit never replaces the payer. |
| `target` / `origin()` | This activation's hit result and trigger location. |
| `incomingDirection` | Captured carrier direction, separate from the executor's live facing. |

## Resolving and deferring

```java
CastContext trigger = CastContext.of(arsContext, hit, providerAccount);
CastContexts.bind(arsContext, trigger);
CastContext action = trigger.withExecutor(recipient);
CastContexts.scoped(action, () -> applyNativeAction(action));
```

`of` reads the origin attached to the Ars context. A non-null account explicitly selects this provider's resource pool; null retains the attached account. Providers with different resources should supply their own account rather than inheriting a foreign pool by accident. Core neither converts resource units nor infers Ars/Iron pool aliases.

`bind` writes an immutable origin through Ars' existing `IContextAttachment` map. Both Ars 4 and Ars 5 copy that map when cloning, including the usual child and delayed contexts. Changing one sibling's attachment does not alter another sibling. The origin stores caster, damage owner and account; executor and target are local to an activation. Reading the next hit therefore defaults its executor to the original caster, and the next provider explicitly chooses any proxy executor it needs.

If an integration constructs a fresh context instead of cloning it, it must explicitly bind the desired origin to that context. Core does not silently inherit ambient ownership into independent casts.

## Sessions and continuations

`ExecutionSession` accepts a `CastContext` and exposes it via `context()`. The legacy caster constructor remains available. Start, permission checks, ticks, activation and cleanup restore this context around the provider's own scope. Account configuration is allowed only before start. A living executor's death, removal or dimension change invalidates the session along with original caster loss.

Impact continuation receives a scoped context with the new hit and flight direction. Executor resets to the original caster; a later breath or motion glyph can select a different executor. A provider continuing its own resolver can use:

```java
var child = frozenResolver.clone();
CastContexts.bind(child.spellContext, CastContexts.current());
child.onResolveEffect(world, hit);
```

`CastContexts.current()` is valid only inside synchronous scoped callbacks. Nested calls and exceptions restore the previous scope. Deferred code retains an immutable context explicitly, rather than expecting thread-local state to survive across ticks. Hit position is a trigger snapshot; provider-owned pose/aim code samples live position and facing when required.

## Iron integration and persistence

Iron's `Resolution` and `CastSession` expose this shared context. Effect permissions, levels and power use its caster; native projectiles and direct damage use its damage owner. Breath/ray/timed actions explicitly choose their executor and preserve the account for upkeep. Motion pulses reconstruct the same roles while retaining native Iron effects.

Ars context attachments are transient runtime data. They are not a universal summon save format. Iron retains its existing leases and motion tags, including a separate damage-owner UUID, and reconstructs roles when those effects resume. Arbitrary resource adapters are not serialized by Core. Other providers retain responsibility for their own persistence, summon ownership and native controllers.
