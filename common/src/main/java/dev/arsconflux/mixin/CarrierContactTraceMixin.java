package dev.arsconflux.mixin;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.arsconflux.api.projectile.TrajectoryContacts;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Capture virtual onHit dispatch as well as overrides supplied by optional projectile forms. */
@Mixin(EntityProjectileSpell.class)
public abstract class CarrierContactTraceMixin {
    @Inject(method="canHitEntity",at=@At("HEAD"),cancellable=true)
    private void conflux$excludeOwnVisual(net.minecraft.world.entity.Entity target,CallbackInfoReturnable<Boolean> result){
        if(TrajectoryContacts.isOwnVisual((EntityProjectileSpell)(Object)this,target))result.setReturnValue(false);
    }
    @WrapOperation(method="traceAnyHit",remap=false,at=@At(value="INVOKE",target="Lcom/hollingsworth/arsnouveau/common/entity/EntityProjectileSpell;onHit(Lnet/minecraft/world/phys/HitResult;)V",remap=true))
    private void conflux$contact(EntityProjectileSpell carrier,HitResult hit,Operation<Void> original){
        TrajectoryContacts.collide(carrier,hit,()->original.call(carrier,hit));
    }
}
