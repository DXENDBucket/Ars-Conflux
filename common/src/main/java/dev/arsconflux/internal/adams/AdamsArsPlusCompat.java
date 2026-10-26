package dev.arsconflux.internal.adams;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import dev.arsconflux.api.glyph.AugmentEquivalence;
import java.util.Map;

/** Actual glyph IDs differ between AAP 5 (adamsarsplus) and 6 (ars_nouveau).
 * Identify the installed glyph implementation without linking optional classes or guessing IDs. */
public final class AdamsArsPlusCompat {
    private static final Map<String, AbstractAugment> BASES = Map.of(
            "Amplify", AugmentAmplify.INSTANCE, "Dampen", AugmentDampen.INSTANCE,
            "AOE", AugmentAOE.INSTANCE, "Accelerate", AugmentAccelerate.INSTANCE,
            "ExtendTime", AugmentExtendTime.INSTANCE, "DurationDown", AugmentDurationDown.INSTANCE);
    private AdamsArsPlusCompat() {}
    public static void register() {
        for (var part : GlyphRegistry.getSpellpartMap().values()) {
            if (!(part instanceof AbstractAugment augment)) continue;
            String name = part.getClass().getName();
            if (!name.startsWith("com.adamsmods.adamsarsplus.glyphs.augment_glyph.")
                    && !name.startsWith("adamsmods.adamsarsplus.common.glyphs.augment_glyph.")) continue;
            String simple = part.getClass().getSimpleName();
            for (var entry : BASES.entrySet()) {
                String prefix = "Augment" + entry.getKey();
                double units = simple.equals(prefix + "Two") ? 2 : simple.equals(prefix + "Three") ? 4 : 0;
                if (units != 0) AugmentEquivalence.register(augment.getRegistryName(), entry.getValue().getRegistryName(), units);
            }
            if (simple.equals("AugmentLesserAOE"))
                AugmentEquivalence.register(augment.getRegistryName(), AugmentAOE.INSTANCE.getRegistryName(), -1);
        }
    }
}
