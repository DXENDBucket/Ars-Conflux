package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.api.context.ResolverContexts;
import dev.arsconflux.internal.neg.NegResolverBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "alexthw.not_enough_glyphs.api.ThreadwiseSpellResolver", remap = false)
public abstract class OptionalNegResolverMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void arsConflux$source(SpellContext context, CallbackInfo ci) {
        NegResolverBridge.capture((SpellResolver)(Object)this);
    }
    @Inject(method = "getNewResolver", at = @At("RETURN"), cancellable = true)
    private void arsConflux$derive(SpellContext context, CallbackInfoReturnable<SpellResolver> ci) {
        ci.setReturnValue(ResolverContexts.derived((SpellResolver)(Object)this, ci.getReturnValue()));
    }
}
