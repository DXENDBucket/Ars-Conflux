package dev.arsconflux.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.projectile.ProjectileReflections;
import dev.arsconflux.config.ConfluxStartupConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.entity.ArcaneShieldEntity",remap=false)
public abstract class OptionalZeroShieldReflectionMixin {
    @Shadow public abstract LivingEntity getOwner();
    @Shadow private Vec3 getSurfaceNormal(Entity source) { throw new AssertionError(); }
    @WrapMethod(method="reflectProjectile")
    private boolean arsConflux$reflect(Projectile projectile,Operation<Boolean> original) {
        if(!ConfluxStartupConfig.zero())return original.call(projectile);
        return switch(ProjectileReflections.reflect(projectile,getSurfaceNormal(projectile),getOwner())) {
            case UNMANAGED -> original.call(projectile);
            case REFLECTED -> true;
            case REJECTED -> false; // Native shield absorbs unsupported fixed/owner-bound flights.
        };
    }
}
