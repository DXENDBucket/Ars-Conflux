package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.api.context.ResolverContexts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpellResolver.class, remap = false)
public abstract class ResolverInheritanceMixin {
    @Inject(method = "getNewResolver", at = @At("RETURN"), cancellable = true)
    private void arsConflux$inheritSource(SpellContext context, CallbackInfoReturnable<SpellResolver> ci) {
        ci.setReturnValue(ResolverContexts.derived((SpellResolver)(Object)this, ci.getReturnValue()));
    }
}
