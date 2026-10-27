package dev.arsconflux.internal.zero;

import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import dev.arsconflux.api.projectile.ProjectileControls;
import dev.arsconflux.config.ConfluxStartupConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Zero selects and validates targets; Core only synchronizes an already-approved motion change. */
public final class ZeroMotion {
    private ZeroMotion() {}
    public static boolean hold(Entity target,Vec3 position,SpellContext context) {
        if(!ConfluxStartupConfig.zero())return false;
        var caster=IMultiPhaseCaster.from(context,context.getUnwrappedCaster());
        var owner=caster==null?null:caster.getCastContext();
        return owner!=null && ProjectileControls.hold(target,owner,position);
    }
}
