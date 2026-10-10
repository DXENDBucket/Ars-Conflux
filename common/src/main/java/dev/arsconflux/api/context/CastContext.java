package dev.arsconflux.api.context;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import dev.arsconflux.api.resource.ResourceAccount;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Explicit roles for adapters; selecting an entity does not implicitly transfer ownership or payment.
 * A non-entity Ars source may have no LivingEntity caster. Account may be absent for free effects. */
public record CastContext(SpellContext ars, LivingEntity caster, LivingEntity executor,
                          LivingEntity damageOwner, HitResult target, Vec3 incomingDirection,
                          ResourceAccount account) {
    public static CastContext of(SpellContext ars, HitResult target, ResourceAccount account) {
        return CastContexts.resolve(ars, target, account);
    }
    public static CastContext detached(LivingEntity caster, HitResult target, ResourceAccount account) {
        return new CastContext(null, caster, caster, caster, target, Vec3.ZERO, account);
    }
    public CastContext withExecutor(LivingEntity entity) {
        return new CastContext(ars, caster, entity, damageOwner, target, incomingDirection, account);
    }
    public CastContext withTarget(HitResult hit) {
        return new CastContext(ars, caster, executor, damageOwner, hit, incomingDirection, account);
    }
    public CastContext withDamageOwner(LivingEntity owner) {
        return new CastContext(ars, caster, executor, owner, target, incomingDirection, account);
    }
    public CastContext withAccount(ResourceAccount resource) {
        return new CastContext(ars, caster, executor, damageOwner, target, incomingDirection, resource);
    }
    public CastContext withIncomingDirection(Vec3 direction) {
        return new CastContext(ars, caster, executor, damageOwner, target,
                direction == null ? Vec3.ZERO : direction.normalize(), account);
    }
    /** Entity selection alone never turns that entity into an executor. */
    public Vec3 origin() {
        return target != null ? target.getLocation() : executor != null ? executor.getEyePosition() : Vec3.ZERO;
    }
}
