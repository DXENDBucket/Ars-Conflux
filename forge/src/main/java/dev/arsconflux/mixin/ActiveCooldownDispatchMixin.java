package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.api.casting.ActiveCasts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpellResolver.class, remap = false)
public abstract class ActiveCooldownDispatchMixin {
    @Inject(method = {"onCast", "onCastOnBlock", "onCastOnEntity"}, at = @At("HEAD"), cancellable = true)
    private void arsConflux$check(CallbackInfoReturnable<Boolean> ci) {
        if (!ActiveCasts.allowDispatch((SpellResolver)(Object)this)) ci.setReturnValue(false);
    }
    @Inject(method = {"onCast", "onCastOnBlock", "onCastOnEntity"}, at = @At("RETURN"))
    private void arsConflux$released(CallbackInfoReturnable<Boolean> ci) {
        ActiveCasts.dispatched((SpellResolver)(Object)this, ci.getReturnValueZ());
    }
}
