package dev.arsconflux.mixin;

import com.github.ars_zero.common.attachment.GravitySuppressionAttachment;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.projectile.ProjectileControls;
import dev.arsconflux.config.ConfluxStartupConfig;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.gravity.GravitySuppression",remap=false)
public abstract class OptionalZeroGravityMotionMixin {
    @WrapMethod(method="enforce")
    private static void arsConflux$suppress(Entity target,GravitySuppressionAttachment attachment,Operation<Void> original) {
        original.call(target,attachment);
        if(ConfluxStartupConfig.zero() && attachment.isActive() && !target.level().isClientSide)ProjectileControls.suppressGravity(target);
    }
    @WrapMethod(method="restore")
    private static void arsConflux$restore(Entity target,GravitySuppressionAttachment attachment,Operation<Void> original) {
        original.call(target,attachment);
        if(attachment.isActive() && !target.level().isClientSide)ProjectileControls.restoreGravity(target);
    }
}
