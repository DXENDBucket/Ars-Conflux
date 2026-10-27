package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.event.SpellResolveEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.internal.zero.ZeroTemporal;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.github.ars_zero.event.ArsZeroResolverEvents",remap=false)
public abstract class OptionalZeroTemporalEventsMixin {
    @WrapMethod(method="onEffectResolving")
    private static void arsConflux$staleCapture(EffectResolveEvent.Pre event,Operation<Void> original) {
        if(!ZeroTemporal.stale(event.resolver.spellContext))original.call(event);
    }
    @WrapMethod(method="onEffectResolved")
    private static void arsConflux$output(EffectResolveEvent.Post event,Operation<Void> original) {
        ZeroTemporal.observed(event,()->original.call(event));
    }
    @WrapMethod(method="onSpellResolved")
    private static void arsConflux$oldCompletion(SpellResolveEvent.Post event,Operation<Void> original) {
        if(!ZeroTemporal.stale(event.resolver.spellContext))original.call(event);
    }
}
