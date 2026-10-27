package dev.arsconflux.mixin;

import com.github.ars_zero.common.entity.EffectBeamEntity;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import dev.arsconflux.config.ConfluxStartupConfig;
import dev.arsconflux.internal.zero.ZeroBeamBilling;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.entity.EffectBeamEntity",remap=false)
public abstract class OptionalZeroBeamBillingMixin {
    @Inject(method="calculateForwardedSpellManaCost",at=@At("HEAD"),cancellable=true)
    private void arsConflux$forwardPrice(SpellContext context,CallbackInfoReturnable<Double> ci) {
        if(ConfluxStartupConfig.zero())ci.setReturnValue(ZeroBeamBilling.forwardCost(context));
    }
    @Inject(method="consumeManaAndAccumulate",at=@At("HEAD"),cancellable=true)
    private void arsConflux$creative(ServerLevel level,double price,CallbackInfoReturnable<Boolean> ci) {
        var player=((EffectBeamEntity)(Object)this).getCasterPlayer();
        if(ConfluxStartupConfig.zero() && player!=null && player.isCreative())ci.setReturnValue(true);
    }
}
