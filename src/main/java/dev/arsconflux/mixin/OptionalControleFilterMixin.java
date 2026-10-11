package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.IFilter;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.internal.controle.RandomOperands;
import it.unimi.dsi.fastutil.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.qther.ars_controle.spell.filter.FilterBinary", remap = false)
public abstract class OptionalControleFilterMixin {
    @Shadow public SpellResolver res;
    @Inject(method = "getFilters", at = @At("RETURN"), cancellable = true)
    private void arsConflux$operands(CallbackInfoReturnable<Pair<Integer, Pair<IFilter, IFilter>>> ci) {
        ci.setReturnValue(RandomOperands.snapshot(res, ci.getReturnValue()));
    }
}
