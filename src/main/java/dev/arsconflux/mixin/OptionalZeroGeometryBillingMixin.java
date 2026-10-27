package dev.arsconflux.mixin;

import com.github.ars_zero.common.glyph.geometrize.EffectGeometrize;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import dev.arsconflux.config.ConfluxStartupConfig;
import dev.arsconflux.internal.zero.ZeroForwardBilling;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.entity.GeometryEntity",remap=false)
public abstract class OptionalZeroGeometryBillingMixin {
    @Inject(method="calculateForwardedSpellManaCost",at=@At("HEAD"),cancellable=true)
    private void arsConflux$quote(SpellContext context,CallbackInfoReturnable<Double> ci) {
        if(ConfluxStartupConfig.zero())ci.setReturnValue(ZeroForwardBilling.price(context,EffectGeometrize.INSTANCE.getGenericResolverManaCostMultiplier()));
    }
    @Inject(method="consumeManaForBlock",at=@At("HEAD"),cancellable=true)
    private void arsConflux$creative(Player player,CallbackInfoReturnable<Boolean> ci) {
        if(ConfluxStartupConfig.zero() && player.isCreative())ci.setReturnValue(true);
    }
}
