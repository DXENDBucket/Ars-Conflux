package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.internal.augments.OptionalAugmentExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets="alexthw.not_enough_glyphs.common.glyphs.EffectFlatten", remap=false)
public abstract class OptionalFlattenAugmentsMixin {
    @WrapMethod(method="doFlat")
    private void arsConflux$block(BlockPos pos, BlockHitResult hit, Level world, SpellStats stats, Operation<Void> original) {
        original.call(pos, OptionalAugmentExtensions.blockHit(pos, hit), world, stats);
    }
}
