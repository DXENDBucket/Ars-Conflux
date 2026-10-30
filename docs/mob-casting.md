# Native non-player mana and planning queries

Core 0.1.45. These APIs support Ars Instinct and content-owned casters without installing AI in Core.

## Native storage, one lifecycle

`ArsManaPools.of(LivingEntity)` exposes the selected native Ars capability in player mana units. On non-players Core provides a lazy fallback: Ars 5's persistent native mana attachment; Ars 4's native ManaCap serialized as a capability. Player storage/ticks remain native. Existing attachments and restored empty pools are preserved, and querying/changing capacity never refills an established pool.

Only activated, alive, server-side non-players are refreshed once per second, staggered by entity ID. No world scan or global entity registry. If the selected capability belongs to another provider, Core does not regenerate it. Fallback-provider selection follows loader capability registration ordering; another provider should register ahead of the fallback and retain ownership. For foreign non-player pools, regeneration is not introspectable and the facade reports zero rather than mutating their attributes.

Default base capacity and regeneration come from Ars server settings. Glyph/book bonuses, native mana attributes and MaxManaCalcEvent/ManaRegenCalcEvent apply. Actual capacity clamps at zero/int range and reserve fractions are applied. Temporary base modifiers have fixed identities, not accumulating copies.

```java
MobMana.configure(actor, new MobMana.Profile(240, 12)); // explicit persisted override
MobMana.registerPolicy(id, 100, actor -> matchesSpecies(actor) ? profile : null);
var pool = ArsManaPools.of(actor);
ArsManaPools.restore(actor, amount);
ArsManaPools.transfer(from, to, amount);
```

Explicit entity settings take precedence over policies. Policies are initialization-time declarations, ordered by descending priority then ID, with duplicate IDs rejected. They do not retain entities. The pack keeps species definitions outside Core. `trySpend` is a low-level amount operation, not a spell quote; transfers always consume actual unreserved donor stock, even for creative players, and do not receive cost discounts. Fractional native quantities are preserved. Explicit profile overrides apply to Core-owned lifecycle only, never a selected foreign provider.

## Read-only planning facts

- `ResourceAccount.capacity()`: physical capacity in provider integer units; **-1 is unknown**, not infinite.
- `SpellBudgets.estimate(context)`: combines the native entry price and one initial activation per triggered glyph by equal reservation keys. Native Ars trigger billing includes deferred native effect prices in this first-hit estimate. Quotes do not reserve or spend.
- `complete`: all initial prices known; `affordable`: combined current balances suffice; `possible`: known capacities suffice (unknown capacity is only accepted when already affordable). This prevents unfundable/unknown accounts from permanently reserving the AI.
- This is not a guarantee that every scatter branch or future channel tick can be funded. Actual providers continue paying on their real activation boundaries.
- `ActiveCastProvider.cooldowns(request)` optionally returns `CooldownQuote(key, finalDuration, remaining)`. Values include native/provider and Conflux timing reductions already. `ActiveCasts.cooldowns(...)` merges shared keys once, read-only. It never imposes a second generic cooldown on a provider.
- Existing structured rejection and actual release callbacks remain authoritative. Old providers keep working via default methods, even when they do not publish planning facts.

## Provider boundaries

Iron 0.17.36 adds native-derived non-player cooldowns in a persistent provider namespace. Player cooldowns still use Iron's real records/events and creative settings. Recurring-burst per-shot reductions and general timing reduction apply once. Ordinary living entities receive native Iron spell attributes. Mana cost configuration retains its existing source selection; NPCs continue using the real Ars account.

Goety 0.1.7 publishes native focus keys/durations and supplies non-player persistent focus cooldowns. Unlimited-fire exceptions remain native. Soul mode requires a usable soul account; a mob without one has capacity zero and cannot bypass it. Ars mode uses the actual Ars pool. No fake-player soul account or automatic currency conversion is created.

`GlyphConditions` optionally supplies a quiet, side-effect-free usefulness predicate per glyph. It guides AI only, never replaces native permissions or changes manual casting. Native Ars Heal rejects undead as healing recipients; Iron Oakskin/Charge avoid redundant buffs. Unknown predicates default to true, while Instinct still skips unknown ability labels.

For Core-managed non-player native Ars resolutions, the Harm compatibility proxy no longer receives damage credit instead of the actual caster. This is scoped to a matching native proxy and real non-player context, retaining the DamageSource instance/type/subtype. World/device proxies and real players are unaffected.

## Validation boundary

Shared APIs and platform wiring target NeoForge 1.21.1 and Forge 1.20.1. Exact checks, JAR hashes and unverified boundaries are in the Instinct implementation handoff. No client HUD or visual compatibility is implied by server tests.
