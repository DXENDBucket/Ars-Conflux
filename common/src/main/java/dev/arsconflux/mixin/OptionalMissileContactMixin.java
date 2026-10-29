package dev.arsconflux.mixin;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.projectile.TrajectoryContacts;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
/** Groups both collision splash and the native final/empty-area pulse, without an optional class link. */
@Pseudo @Mixin(targets="alexthw.not_enough_glyphs.common.spell.MissileProjectile",remap=false)
public abstract class OptionalMissileContactMixin {
    @WrapMethod(method="activateSpellAtPos")
    private void conflux$pulse(Vec3 point,Operation<Void> original){
        TrajectoryContacts.pulse((EntityProjectileSpell)(Object)this,()->original.call(point));
    }
}
