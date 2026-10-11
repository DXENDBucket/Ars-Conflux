package dev.arsconflux.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Test-only inner operation: exercise the real production registration branch in a dev launcher. */
@Pseudo
@Mixin(targets = "alexthw.ars_elemental.ArsNouveauRegistry", remap = false, priority = 500)
public abstract class ElementalProductionGateTestMixin {
    @WrapOperation(method = "registerGlyphs", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/fml/loading/FMLLoader;isProduction()Z"))
    private static boolean arsConfluxTest$production(Operation<Boolean> original) {
        System.setProperty("arsconflux.test.production_seen", "true");
        return true;
    }
}
