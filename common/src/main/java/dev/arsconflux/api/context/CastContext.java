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
        var caster = ars.getUnwrappedCaster();
        return new CastContext(ars, caster, caster, caster, target, TriggerGeometry.read(ars), account);
    }
    public CastContext withExecutor(LivingEntity entity) {
        return new CastContext(ars, caster, entity, damageOwner, target, incomingDirection, account);
    }
}
