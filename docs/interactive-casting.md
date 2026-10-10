# Interactive casting (0.1.9)

This API describes input, identity, execution lifetime and suspended composition. Native spell rules belong to providers. It has no dependency on Iron or Goety and does not identify their spells.

## Lifecycle and responsibility

`InputSession<P>` decorates the existing `ExecutionSession<P>` and uses `ExecutionSessions` for ticking and cleanup. It is not another scheduler. `InputSessions` only indexes endpoints for input routing. Currently an actor can own one active interactive endpoint; ordinary background executions may coexist.

- `CastInput`: ACTIVATE / CONFIRM / CANCEL, optional hand, origin, normalized direction and optional hit result. The provider selects targets when no hit is supplied.
- `InputBinding`: source identity and validity. `held(actor, hand, spell)` binds the actual held stack, a component/NBT snapshot and ordered glyph IDs. Another hand/actor cannot advance it; switching the owned source or selected recipe cancels the old endpoint.
- `InteractiveDriver<P>`: provider-owned parameters/stage state, start/tick/input/close callbacks. PASS lets ordinary input continue; REJECTED consumes it without advancement; ACCEPTED advances according to the provider; COMPLETED closes the session.
- `ExecutionSession.renew(ticks)`: update the lease deadline after accepted input. Age and resource payments are unchanged. Expired input cannot renew a session.
- `ExecutionScope`: restore provider-local authorization or other temporary execution context around every callback, including deferred completion.

Death/removal, executor loss, leaving the original world, server shutdown and world unload retain the execution lifecycle's cancellation rules. Binding or permission failure also cancels delayed completion. These transient sessions are not saved to disk. Cross-dimension selection needs a different lifetime policy; it is not implicitly supported by this initial API.

## Suspending an Ars composition

Capture the remainder at the current resolver cursor **before** calling `spellContext.stop()`:

```java
var remainder = ArsContinuation.capture(resolver);
var endpoint = new InputSession<>(world, castContext, parameters, timeout,
    InputBinding.held(actor, hand, resolver.spell), driver, remainder, permission);
if (InputSessions.start(endpoint)) resolver.spellContext.stop();
```

The provider's successful close callback can call `remainder.resolve(finalHit)`. The snapshot resumes once at the saved cursor, using the final hit and saved caster/executor/damage-owner/account. It calls `resume`, not `onResolveEffect`: preceding glyphs and the ordinary active-cast entry are not replayed. Cancellation should leave the remainder unresolved; the execution closes it after the driver's close callback. `capture` itself neither stops the original resolver nor starts a session.

The provider must only stop a successfully armed execution. If activation fails, close the unused snapshot and let its own effect failure policy apply. Any native world effects already produced are the provider's responsibility to clean up; Core cannot roll those back.

## Input entry and resource rules

Standard Ars 5 active casters / scry casters and Ars 4 `SpellCaster` / scry casters route matching input before their ordinary dispatch. A matching sneak activation maps to CANCEL. A custom tool with its own entry should call `InputSessions.route(actor, hand, spell)` **before** its mana, chanting and cooldown checks. Direct automated callers may use `endpoint.submit(CastInput)` with their own binding and permission policy.

Sessions default to free routing. They do not infer fees from native spell IDs. Providers can use the existing resource transaction and execution billing API, decide which stages cost resources and which accepted stages renew the timeout, and commit native cooldowns at their own completion boundary. The optional vanilla Ars trigger-billing configuration keeps its existing semantics.

There is no generic wait glyph, client selection overlay, mouse-release transport or automatic AI planner in this release. ACTIVATE is wired to normal active use; CONFIRM is available to API callers. Merely installing Core does not change ordinary single-use Ars spells.

## Focused verification

The `ars_conflux_interaction` test namespace checks routing/lease renewal/context, source/recipe cancellation, and one-shot cursor continuation without a spell provider. Iron's separate `irons_nouveau_interaction` namespace exercises a native multi-input effect through the public API and the actual Ars casting entry on both platforms. Client visuals still require in-game inspection.

0.1.9 release checks: NeoForge standalone Core passed all 3 interaction tests with no native spell provider installed; Iron integration passed all 4 targeted checks on each platform. Matching release JARs were built, Forge reobfuscated, and license/API/asset presence checked. These are server checks, not a client visual test.
