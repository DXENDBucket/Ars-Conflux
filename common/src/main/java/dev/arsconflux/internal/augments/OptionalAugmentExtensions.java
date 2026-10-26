package dev.arsconflux.internal.augments;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import dev.arsconflux.config.ConfluxStartupConfig;
import dev.arsconflux.platform.ArsAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Small optional upstream repairs. No optional classes are linked or copied. */
public final class OptionalAugmentExtensions {
    private OptionalAugmentExtensions() {}
    public static void register() {
        if (!ConfluxStartupConfig.augmentExtensions()) return;
        for (var part : GlyphRegistry.getSpellpartMap().values()) {
            String name = part.getClass().getName();
            if (name.endsWith(".EffectAnnihilate") && name.contains("adamsarsplus.")) {
                part.compatibleAugments.addAll(java.util.Set.of(AugmentAOE.INSTANCE, AugmentPierce.INSTANCE, AugmentSensitive.INSTANCE));
            } else if (name.endsWith(".EffectDivineSmite") && name.contains("adamsarsplus.")) {
                part.compatibleAugments.addAll(java.util.Set.of(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE));
                ArsAccess.augmentDescription(part, AugmentExtendTime.INSTANCE, "ars_conflux.augment.shock_duration_up");
                ArsAccess.augmentDescription(part, AugmentDurationDown.INSTANCE, "ars_conflux.augment.shock_duration_down");
            } else if (name.startsWith("alexthw.not_enough_glyphs.") && name.endsWith(".EffectFlatten")) {
                part.compatibleAugments.addAll(java.util.Set.of(AugmentAOE.INSTANCE, AugmentPierce.INSTANCE));
            }
        }
    }
    public static BlockHitResult blockHit(BlockPos pos, BlockHitResult original) {
        if (!ConfluxStartupConfig.augmentExtensions()) return original;
        var delta = Vec3.atLowerCornerOf(pos.subtract(original.getBlockPos()));
        return new BlockHitResult(original.getLocation().add(delta), original.getDirection(), pos, original.isInside());
    }
    public static int duration(int ticks) {
        return ConfluxStartupConfig.augmentExtensions() ? Math.max(1, ticks) : ticks;
    }
}
