package dev.arsconflux.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.arsconflux.config.ConfluxStartupConfig;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.glyph.SelectEffect",remap=false)
public abstract class OptionalZeroSelectHeightMixin {
    @ModifyExpressionValue(method="onResolveBlock",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;isOutsideBuildHeight(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean arsConflux$insideHeight(boolean outside) {
        // Released 2.0.2 negates this predicate before skipping: correct that one call site.
        return ConfluxStartupConfig.zeroFixes() ? !outside : outside;
    }
}
