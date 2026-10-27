package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.internal.zero.ZeroTemporal;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.glyph.geometrize.EffectGeometrize",remap=false)
public abstract class OptionalZeroGeometryTargetsMixin {
    @WrapMethod(method="updateTemporalContext")
    private void arsConflux$one(LivingEntity caster,Entity geometry,SpellContext context,Operation<Void> original) {
        ZeroTemporal.produced(context,()->original.call(caster,geometry,context));
    }
}
