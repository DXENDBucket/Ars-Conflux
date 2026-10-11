package dev.arsconflux.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.context.RetaliationTargets;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class RetaliationDamageMixin {
    @WrapMethod(method = "hurt")
    private boolean arsConflux$damage(DamageSource source, float amount, Operation<Boolean> original) {
        return RetaliationTargets.during((LivingEntity)(Object)this, source, () -> original.call(source, amount));
    }
}
