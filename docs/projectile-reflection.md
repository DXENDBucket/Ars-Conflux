# Managed projectile reflection

Available in Conflux 0.1.35 on both loaders. A shield adapter calls:

```java
switch (ProjectileReflections.reflect(projectile, surfaceNormal, reflector)) {
    case UNMANAGED -> nativeReflection();
    case REFLECTED -> successfulReflection();
    case REJECTED -> nativeAbsorption();
}
```

Call on the server thread before discarding the intercepted projectile. The normal need not be unit length. Invalid movement, inactive sessions and unsupported trajectories return `REJECTED` before modifying the flight. Zero 2.0.2 uses this adapter on NeoForge only; the public API does not depend on Zero.

## Ownership and motion

- A tracked native projectile, its `CarrierProxy`, and an off-world `ArsTrajectory` carrier resolve to the same flight. A normal unmanaged Ars/native projectile returns `UNMANAGED`.
- Ordinary linear/gravity carriers are eligible. Ars homing carriers release their old target, preserve original target predicates for other entities, allow the original shooter and exclude the new owner's allies. Subclasses with other motion rules should override the opt-in contract appropriately.
- A session can transfer ownership only when all its active tracked projectiles share this carrier. A native salvo using one carrier reflects as a cohort and retains relative offsets. Independent flights in one session are rejected rather than changing unrelated siblings.
- Resolver context is forked; native projectile owners and explicitly bound native-damage contexts are updated. The reflector becomes damage owner. Caster, executor, source power, eligibility, payment account and original lifecycle remain unchanged. In particular, the original caster's death or departure still ends the existing execution.
- Reflected Ars dispatch uses an explicit scope to project damage credit while retaining native damage-source class, type and metadata. Sources unrelated to the old caster/executor are not rewritten. No extra launch cost or cooldown is charged for reflection.

## Extending a carrier

Fixed, repeated or owner-relative motion is rejected unless the carrier implements `ReflectableCarrier` and explicitly supports redirection. Its `canReflect()` must be safe to query before mutation. After the API sets velocity, owner and resolver, `reflected(previousOwner, reflector)` clears or rebuilds provider-specific target/path state. This callback must not throw or spend resources. A homing subclass with an incompatible movement model should return false from `canReflect()`.

Orbit, Echo Trail and Carian Phalanx are not opted in by this release. Providers still own native payload/secondary-effect attribution and friend/enemy eligibility; this API does not rewrite every provider's target checks. Delayed children must inherit the reflected context through the existing context APIs.

## Validation

Pure tests cover speed-preserving reflection, nonunit normals, invalid vectors, repeated reflection, cohort identity and scoped credit rules. Released-JAR checks cover mixin methods and descriptors. Both loader artifacts are built, including Forge SRG mappings. These checks do not establish runtime Mixin application, visual correctness or multiplayer behavior; those remain manual verification items.
