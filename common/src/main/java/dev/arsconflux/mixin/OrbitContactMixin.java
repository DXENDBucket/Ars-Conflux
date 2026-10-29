package dev.arsconflux.mixin;
import com.hollingsworth.arsnouveau.common.entity.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.projectile.TrajectoryContacts;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(EntityOrbitProjectile.class)
public abstract class OrbitContactMixin {
    @WrapMethod(method="onHit")
    private void conflux$contact(HitResult hit,Operation<Void> original){
        TrajectoryContacts.collide((EntityProjectileSpell)(Object)this,hit,()->original.call(hit));
    }
}
