package dev.arsconflux.mixin;

import dev.arsconflux.internal.augments.OptionalAugmentExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets="adamsmods.adamsarsplus.common.entity.EntityDivineSmite", remap=false)
public abstract class OptionalDivineSmiteAugmentsMixin {
    @ModifyArg(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/world/effect/MobEffectInstance;<init>(Lnet/minecraft/core/Holder;II)V"), index=1)
    private int arsConflux$duration(int ticks) { return OptionalAugmentExtensions.duration(ticks); }
}
