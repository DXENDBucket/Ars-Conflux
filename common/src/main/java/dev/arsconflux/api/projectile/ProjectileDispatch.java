package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.platform.ArsAccess;
import net.minecraft.world.entity.Entity;

/** A single Ars spawn hook dispatches to any participating addon without pairwise compatibility. */
public final class ProjectileDispatch {
    private ProjectileDispatch() {}
    public static boolean convert(Entity entity) {
        // Client spawn packets contain a visual context, not a server caster lookup. Let those
        // entities join normally; never read a resolver, dispatch a payload or bill on the client.
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel)) return false;
        if (!(entity instanceof EntityProjectileSpell projectile)) return false;
        var resolver = ArsAccess.resolver(projectile);
        if (resolver == null) return false;
        var forms = resolver.spell.getAugments(0, resolver.spellContext.getUnwrappedCaster()).stream()
                .filter(ProjectileForm.class::isInstance).map(ProjectileForm.class::cast).toList();
        if (forms.isEmpty()) return false;
        if (forms.size() != 1) return true; // Ambiguous payload: fail closed even on a raw/generated recipe.
        var profile = CarrierRegistry.of(projectile);
        return profile != null && forms.get(0).replace(projectile, profile);
    }
}
