package dev.arsconflux.mixin;

import com.github.ars_zero.common.entity.EffectBeamEntity;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.internal.zero.ZeroTemporal;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.glyph.EffectBeam",remap=false)
public abstract class OptionalZeroBeamTargetsMixin {
    @WrapMethod(method="updateTemporalContext")
    private void arsConflux$one(LivingEntity caster,EffectBeamEntity beam,SpellContext context,Operation<Void> original) {
        ZeroTemporal.produced(context,()->original.call(caster,beam,context));
    }
    @WrapMethod(method="updateTemporalContextMultiple")
    private void arsConflux$many(LivingEntity caster,List<EffectBeamEntity> beams,SpellContext context,Operation<Void> original) {
        ZeroTemporal.produced(context,()->original.call(caster,beams,context));
    }
}
