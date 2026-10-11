package dev.arsconflux.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsconflux.config.ConfluxStartupConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Change only Elemental's glyph-registration gate, never the loader's global production flag. */
@Pseudo
@Mixin(targets = "alexthw.ars_elemental.ArsNouveauRegistry", remap = false)
public abstract class OptionalElementalRegistrationMixin {
    @WrapOperation(method = "registerGlyphs", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/neoforged/fml/loading/FMLLoader;isProduction()Z"))
    private static boolean arsConflux$includePhalanx(Operation<Boolean> original) {
        return original.call() && !ConfluxStartupConfig.elementalPhalanx();
    }
}
