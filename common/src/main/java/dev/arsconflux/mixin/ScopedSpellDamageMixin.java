package dev.arsconflux.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.damage.NativeSpellDamage;
import dev.arsconflux.api.damage.SpellPower;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Dormant outside explicit native scopes or spell-created payloads. */
@Mixin(LivingEntity.class)
public abstract class ScopedSpellDamageMixin {
    @WrapMethod(method="hurt")
    private boolean arsConflux$scopedDamage(DamageSource source,float amount,Operation<Boolean> original) {
        var target=(LivingEntity)(Object)this;
        return dev.arsconflux.api.projectile.ImpactTriggers.damage(target,()->
                NativeSpellDamage.hurt(NativeSpellDamage.currentScope(),target,source,amount,
                        (s,n)->SpellPower.hurt(target,s,n,(next,value)->original.call(next,value))));
    }
}
