package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.projectile.ProjectileControls;
import dev.arsconflux.internal.projectile.MotionControlData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opt-in controls only; ordinary unmodified Ars projectiles execute their native tick. */
@Mixin(EntityProjectileSpell.class)
public abstract class ProjectileControlTickMixin {
    @Inject(method="isNoGravity",at=@At("RETURN"),cancellable=true)
    private void arsConflux$syncedGravity(CallbackInfoReturnable<Boolean> ci) {
        if(((MotionControlData)this).conflux$motionFlags()!=0)ci.setReturnValue(true);
    }
    @WrapMethod(method="tick")
    private void arsConflux$controlledTick(Operation<Void> original) {
        var carrier=(EntityProjectileSpell)(Object)this;
        if(ProjectileControls.beforeNativeTick(carrier)) {
            if(++carrier.age>carrier.getExpirationTime())carrier.discard();
            return;
        }
        original.call();
        ProjectileControls.afterNativeTick(carrier);
    }
}
