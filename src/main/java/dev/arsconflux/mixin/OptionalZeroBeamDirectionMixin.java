package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.arsconflux.api.context.ResolverContexts;
import dev.arsconflux.config.ConfluxStartupConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.entity.EffectBeamEntity",remap=false)
public abstract class OptionalZeroBeamDirectionMixin {
    @Shadow public abstract Vec3 getForward();
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;onResolveEffect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/HitResult;)V"))
    private void arsConflux$direction(SpellResolver resolver,Level world,HitResult hit,Operation<Void> original) {
        if(ConfluxStartupConfig.zero())ResolverContexts.resolveFrom(resolver,world,hit,getForward());
        else original.call(resolver,world,hit);
    }
}
