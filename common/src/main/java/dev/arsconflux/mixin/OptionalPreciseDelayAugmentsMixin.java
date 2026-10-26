package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsconflux.api.glyph.AugmentEquivalence;
import dev.arsconflux.config.ConfluxStartupConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="dev.qther.ars_controle.spell.effect.EffectPreciseDelay", remap=false)
public abstract class OptionalPreciseDelayAugmentsMixin {
    @WrapOperation(method="sendPacket", at=@At(value="INVOKE", target="Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;getBuffCount(Lcom/hollingsworth/arsnouveau/api/spell/AbstractAugment;)I"))
    private int arsConflux$weighted(SpellStats stats, AbstractAugment augment, Operation<Integer> original) {
        if (!ConfluxStartupConfig.augmentExtensions() || augment != AugmentExtendTime.INSTANCE) return original.call(stats, augment);
        // Retain the native 20-Extend-Time bound; an int shift must never wrap for high-tier equivalents.
        return (int)Math.min(20, Math.max(0, Math.round(AugmentEquivalence.sum(stats.getAugments(), augment))));
    }
}
