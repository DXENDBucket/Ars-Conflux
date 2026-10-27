package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.arsconflux.internal.zero.ZeroTemporal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.glyph.TemporalContextForm",remap=false)
public abstract class OptionalZeroTemporalFormMixin {
    @WrapMethod(method="resolveFromStoredContext")
    private CastResolveType arsConflux$input(Level level,LivingEntity caster,SpellContext context,SpellResolver resolver,Operation<CastResolveType> original) {
        return ZeroTemporal.resolve(context,()->original.call(level,caster,context,resolver));
    }
    @WrapOperation(method="resolveFromStoredContext",at=@At(value="INVOKE",target="Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;onResolveEffect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/HitResult;)V"))
    private void arsConflux$oneTarget(SpellResolver resolver,Level level,HitResult hit,Operation<Void> original) {
        ZeroTemporal.target(resolver,hit,()->original.call(resolver,level,hit));
    }
}
