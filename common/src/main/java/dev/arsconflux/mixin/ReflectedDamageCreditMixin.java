package dev.arsconflux.mixin;

import dev.arsconflux.api.projectile.ProjectileReflections;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native source subtypes/metadata remain intact; no global mutation of cached damage sources. */
@Mixin(DamageSource.class)
public abstract class ReflectedDamageCreditMixin {
    @Inject(method="getEntity",at=@At("RETURN"),cancellable=true)
    private void arsConflux$credit(CallbackInfoReturnable<Entity> ci) {
        var declared=ci.getReturnValue(); var reflected=dev.arsconflux.internal.mana.MobDamageCredit.resolve(ProjectileReflections.damageOwner(declared));
        if(reflected!=declared)ci.setReturnValue(reflected);
    }
}
