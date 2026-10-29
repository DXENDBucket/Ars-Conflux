package dev.arsconflux.mixin;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.projectile.TrajectoryContacts;
import org.spongepowered.asm.mixin.*;
@Pseudo @Mixin(targets="alexthw.not_enough_glyphs.common.spell.TrailingProjectile",remap=false)
public abstract class OptionalTrailContactMixin {
    @WrapMethod(method="castSpells")
    private void conflux$pulse(Operation<Void> original){
        TrajectoryContacts.pulse((EntityProjectileSpell)(Object)this,()->original.call());
    }
}
