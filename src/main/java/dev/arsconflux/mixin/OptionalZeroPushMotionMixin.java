package dev.arsconflux.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.arsconflux.api.projectile.ProjectileControls;
import dev.arsconflux.config.ConfluxStartupConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.glyph.PushEffect",remap=false)
public abstract class OptionalZeroPushMotionMixin {
    @WrapOperation(method="onResolveEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void arsConflux$push(Entity target,Vec3 velocity,Operation<Void> original) {
        if(!ConfluxStartupConfig.zero() || !ProjectileControls.velocity(target,velocity))original.call(target,velocity);
    }
}
