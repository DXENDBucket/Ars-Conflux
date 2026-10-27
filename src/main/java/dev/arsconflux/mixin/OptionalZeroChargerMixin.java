package dev.arsconflux.mixin;

import com.github.ars_zero.common.entity.SourceJarChargerEntity;
import com.github.ars_zero.common.glyph.convergence.EffectConvergence;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.arsconflux.config.ConfluxStartupConfig;
import dev.arsconflux.internal.zero.ZeroSourceBilling;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.glyph.convergence.ChargerHelper",remap=false)
public abstract class OptionalZeroChargerMixin {
    @WrapOperation(method="handleBlockCharger",at=@At(value="INVOKE",target="Lcom/github/ars_zero/common/entity/SourceJarChargerEntity;setInitialSourceAmount(I)V"))
    private static void arsConflux$allowance(SourceJarChargerEntity charger,int nativeAmount,Operation<Void> original,
            @Local(argsOnly=true) SpellResolver resolver,@Local(argsOnly=true) EffectConvergence effect) {
        original.call(charger,ConfluxStartupConfig.zeroFixes() ? ZeroSourceBilling.claimInitialSource(resolver,effect) : nativeAmount);
    }
    @WrapOperation(method="handleBlockCharger",at=@At(value="INVOKE",target="Lcom/github/ars_zero/common/entity/SourceJarChargerEntity;setSourceOriginPos(Lnet/minecraft/core/BlockPos;)V"))
    private static void arsConflux$origin(SourceJarChargerEntity charger,BlockPos nativePos,Operation<Void> original,
            @Local(argsOnly=true) SpellContext context) {
        original.call(charger,ConfluxStartupConfig.zeroFixes() && context.getCaster() instanceof TileCaster tile
                ? tile.getTile().getBlockPos() : nativePos);
    }
}
