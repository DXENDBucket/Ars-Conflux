package dev.arsconflux.mixin;

import dev.arsconflux.internal.hex.HexLinkBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"io.yukkuric.hex_ars_link.action.OpCastFromPlayer$Action", "io.yukkuric.hex_ars_link.action.OpTouchCast$Action", "io.yukkuric.hex_ars_link.action.OpShootCast$Action"}, remap = false)
public abstract class OptionalHexActiveCastMixin {
    @Inject(method = "cast(Lat/petrak/hexcasting/api/casting/eval/CastingEnvironment;)V", at = @At("HEAD"), cancellable = true)
    private void arsConflux$active(@Coerce Object environment, CallbackInfo ci) {
        if (HexLinkBridge.intercept(this, environment)) ci.cancel();
    }
}
