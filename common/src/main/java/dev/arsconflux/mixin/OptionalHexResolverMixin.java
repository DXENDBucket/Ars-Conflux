package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.internal.billing.VanillaTriggerBilling;
import dev.arsconflux.internal.hex.HexLinkBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.yukkuric.hex_ars_link.env.ars.PatternResolver", remap = false)
public abstract class OptionalHexResolverMixin {
    @Inject(method = "<init>(Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lat/petrak/hexcasting/api/casting/eval/CastingEnvironment;I)V", at = @At("TAIL"))
    private void arsConflux$capture(SpellContext context, @Coerce Object environment, int omission, CallbackInfo ci) {
        HexLinkBridge.capture((SpellResolver)(Object)this, environment, omission);
    }
    @Inject(method = "getResolveCost", at = @At("HEAD"), cancellable = true)
    private void arsConflux$frozen(CallbackInfoReturnable<Integer> ci) {
        var cost = VanillaTriggerBilling.frozenQuoteCost(((SpellResolver)(Object)this).spellContext, false);
        if (cost != null) ci.setReturnValue(cost);
    }
    @Inject(method = "getResolveCostBase", at = @At("RETURN"), cancellable = true)
    private void arsConflux$nonnegative(CallbackInfoReturnable<Integer> ci) { ci.setReturnValue(Math.max(0, ci.getReturnValue())); }
    @Inject(method = "getMediaCost", at = @At("HEAD"), cancellable = true)
    private void arsConflux$fullMediaQuote(CallbackInfoReturnable<Integer> ci) { ci.setReturnValue(HexLinkBridge.mediaCost((SpellResolver)(Object)this)); }
}
