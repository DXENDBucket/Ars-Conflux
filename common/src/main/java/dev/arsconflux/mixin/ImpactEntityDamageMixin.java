package dev.arsconflux.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.projectile.ImpactTriggers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;

/** No scaling or event dispatch; inactive outside an explicit impact observation. */
@Mixin(Entity.class)
public abstract class ImpactEntityDamageMixin {
    @WrapMethod(method="hurt")
    private boolean arsConflux$receipt(DamageSource source,float amount,Operation<Boolean> original) {
        return ImpactTriggers.damage((Entity)(Object)this, () -> original.call(source,amount));
    }
}
