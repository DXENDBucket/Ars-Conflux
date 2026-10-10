package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.internal.hex.HexLinkBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.yukkuric.hex_ars_link.env.hex.GlyphCallbackCastEnv$Companion", remap = false)
public abstract class OptionalHexCallbackMixin {
    @Inject(method = "getNewDepthFrom", at = @At("HEAD"), cancellable = true)
    private void arsConflux$sourceDepth(SpellResolver resolver, CallbackInfoReturnable<Integer> ci) {
        var depth = HexLinkBridge.nextCallbackDepth(resolver);
        if (depth != null) ci.setReturnValue(depth);
    }
}
