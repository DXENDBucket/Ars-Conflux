# Projectile controls and native continuation directions

Conflux 0.1.36 exposes provider-neutral server APIs on both loaders. Ars Zero 2.0.2 adapters live only in the NeoForge source set. They preserve native target eligibility, effects and prices.

## Motion

`ProjectileControls` accepts a native Ars `EntityProjectileSpell`, a registered visual in `ArsTrajectory`, or another same-flight visual exposing `CarrierProxy`. It updates the carrier and its native visual cohort together. Unrelated flights in the same execution are excluded. Unsupported entities return false so the adapter can execute its native operation.

| Method | Contract |
| --- | --- |
| `velocity(entity, vector)` | Set the flight's velocity and release the current hold. Retain any gravity suppression. |
| `hold(entity, ownerToken, position)` | Translate the flight so this member reaches the approved position, preserving formation offsets. Pause carrier movement and renew a two-tick lease. |
| `release(entity, ownerToken)` | Release only that holder. An obsolete holder cannot release a newer one. |
| `suppressGravity(entity)` | Keep the carrier weightless and clear vertical velocity, matching Zero's suppression behavior. |
| `restoreGravity(entity)` | End suppression; retain weightlessness if held, otherwise restore the original carrier setting. |

Call these only after the provider has validated position and permissions. Tokens are weakly referenced; producers must retain them while using the hold. Zero uses its existing multi-phase context as the token and releases on physics restoration. Lease expiry prevents abandoned holds from lasting indefinitely.

The carrier's original gravity is captured once across overlapping hold/suppression controls. Ars uses a separate `isNoGravity` field; the API restores it along with the vanilla flag. Native entity metadata synchronizes hold/suppression flags so on-world Ars projectiles obey the same state on the client. Holds pause movement and collisions but continue carrier age and execution lifetime, preventing free lifetime extension. Ordinary projectiles without a control continue their original tick.

This is a motion override, not a new trajectory or ownership system. On release, fixed/owner-relative paths and homing targets retain their native behavior. Native mobs outside the paired projectile mechanism continue using Zero's own movement. No resources, damage credit, cooldowns or permissions transfer.

## Impact direction

`ResolverContexts.resolveFrom(resolver, world, hit, direction)` dispatches the existing resolver with a normalized incoming-direction snapshot. It retains source extensions, targets, origin ownership, payment and native resolver behavior. It updates the execution world and stores the snapshot in the Ars context before dispatch so delayed/cloned children retain it. A zero/invalid direction clears stale stored direction and masks an outer direction scope during that dispatch.

Zero Beam supplies its actual forward vector. Base Voxel and Arcane Voxel supply current velocity. Existing provider targeting rules still decide whether a hit entity becomes an executor; direction inheritance does not turn an enemy into a friendly executor. Stationary execution keeps the provider's ordinary directionless fallback.

## Validation limits

Targeted tests cover hold renewal/expiry, holder identity, release order with overlapping suppression, launch, original weightlessness, nested direction scopes and exceptions, native augment weights and released-JAR hook descriptors. Both loader builds and Forge mappings are checked. These are low-memory checks, not runtime Mixin application or client/multiplayer visual verification.
