package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.internal.zero.ZeroTemporal;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.glyph.convergence.EffectConvergence",remap=false)
public abstract class OptionalZeroConvergenceTargetsMixin {
    @WrapMethod(method="updateTemporalContext")
    private void arsConflux$one(LivingEntity caster,Entity charger,SpellContext context,SpellResolver resolver,Operation<Void> original) {
        ZeroTemporal.producedEntity(context,charger,()->original.call(caster,charger,context,resolver));
    }
}
