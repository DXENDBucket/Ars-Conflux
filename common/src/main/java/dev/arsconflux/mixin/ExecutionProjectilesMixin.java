package dev.arsconflux.mixin;

import dev.arsconflux.api.execution.ExecutionSessions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ExecutionProjectilesMixin {
    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void arsConflux$track(Entity entity, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValueZ()) dev.arsconflux.api.damage.SpellPower.spawned(entity);
        var session = ExecutionSessions.current();
        if (ci.getReturnValueZ() && session != null) session.captureProjectile(entity);
    }
}
