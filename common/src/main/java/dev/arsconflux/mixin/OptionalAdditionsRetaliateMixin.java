package dev.arsconflux.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsconflux.api.context.RetaliationTargets;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.github.jarva.arsadditions.common.glyph.MethodRetaliate", remap = false)
public abstract class OptionalAdditionsRetaliateMixin {
    @WrapOperation(method = "cast", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getKillCredit()Lnet/minecraft/world/entity/LivingEntity;", remap = true))
    private LivingEntity arsConflux$currentAttacker(LivingEntity victim, Operation<LivingEntity> original) {
        return RetaliationTargets.isTakingDamage(victim) ? RetaliationTargets.currentAttacker(victim) : original.call(victim);
    }
}
