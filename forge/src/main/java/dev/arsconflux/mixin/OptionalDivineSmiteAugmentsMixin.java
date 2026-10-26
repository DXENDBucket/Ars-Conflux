package dev.arsconflux.mixin;

import dev.arsconflux.internal.augments.OptionalAugmentExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets="com.adamsmods.adamsarsplus.entities.EntityDivineSmite", remap=false)
public abstract class OptionalDivineSmiteAugmentsMixin {
    @ModifyArg(method={"tick","m_8119_"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/effect/MobEffectInstance;<init>(Lnet/minecraft/world/effect/MobEffect;II)V", remap=true), index=1)
    private int arsConflux$duration(int ticks) { return OptionalAugmentExtensions.duration(ticks); }
}
