package dev.arsconflux.mixin;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.context.TriggerGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = EntityProjectileSpell.class, remap = false)
public abstract class ArsTriggerGeometryMixin {
    @Inject(method = "onHit", remap = true, at = @At("HEAD"))
    private void arsConflux$direction(CallbackInfo ci) {
        var projectile = (EntityProjectileSpell)(Object)this;
        if (!projectile.level().isClientSide && projectile.spellResolver != null)
            TriggerGeometry.write(projectile.spellResolver.spellContext, TriggerGeometry.direction(projectile));
    }
}
