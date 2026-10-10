package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.internal.hex.HexLinkBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ars 5 split requirement and debit; Linker only overrides the former. Ars 4 uses one price. */
@Mixin(value = SpellResolver.class, remap = false)
public abstract class HexDebitMixin {
    @Inject(method = "getExpendedCost", at = @At("RETURN"), cancellable = true)
    private void arsConflux$nativeDebit(CallbackInfoReturnable<Integer> ci) {
        ci.setReturnValue(HexLinkBridge.debit((SpellResolver)(Object)this, ci.getReturnValue()));
    }
}
