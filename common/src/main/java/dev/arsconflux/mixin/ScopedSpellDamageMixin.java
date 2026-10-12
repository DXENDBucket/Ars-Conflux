package dev.arsconflux.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.damage.NativeSpellDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Dormant outside the caller's explicit, synchronous native damage scope. */
@Mixin(LivingEntity.class)
public abstract class ScopedSpellDamageMixin {
    @WrapMethod(method = "hurt")
    private boolean arsConflux$scopedDamage(DamageSource source, float amount, Operation<Boolean> original) {
        return dev.arsconflux.api.projectile.ImpactTriggers.damage((LivingEntity)(Object)this, () ->
                NativeSpellDamage.hurt(NativeSpellDamage.currentScope(), (LivingEntity)(Object)this,
                        source, amount, (modifiedSource, modifiedAmount) -> original.call(modifiedSource, modifiedAmount)));
    }
}
