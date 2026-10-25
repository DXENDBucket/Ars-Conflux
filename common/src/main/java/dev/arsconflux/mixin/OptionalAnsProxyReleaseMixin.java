package dev.arsconflux.mixin;

import dev.arsconflux.internal.ans.AnsActiveCasts;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.otectus.arsnspells.spell.irons.ArsCrossProxySpell", remap = false)
public abstract class OptionalAnsProxyReleaseMixin {
    @Inject(method = "onCast", at = @At("HEAD"), cancellable = true)
    private void arsConflux$release(Level world, int level, LivingEntity caster,
            @Coerce Object source, @Coerce Object data, CallbackInfo ci) {
        if (AnsActiveCasts.release(this, world, level, caster, source, data)) ci.cancel();
    }
}
