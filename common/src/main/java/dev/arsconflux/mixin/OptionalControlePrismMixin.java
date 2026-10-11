package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.projectile.ArsTrajectory;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.qther.ars_controle.block.WarpingSpellPrismBlock", remap = false)
public abstract class OptionalControlePrismMixin {
    @Inject(method = "changeSpellLevel", at = @At("HEAD"), cancellable = true)
    private static void arsConflux$managedTransfer(ServerLevel destination, EntityProjectileSpell carrier,
            CallbackInfoReturnable<EntityProjectileSpell> ci) {
        if (ArsTrajectory.redirect(carrier, destination)) ci.setReturnValue(carrier);
    }
}
