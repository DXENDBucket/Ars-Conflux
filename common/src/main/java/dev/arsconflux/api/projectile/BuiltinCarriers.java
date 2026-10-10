package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import java.util.HashSet;
import java.util.Set;

/** Optional forms are registered by ID only; absent addon classes are never linked. */
public final class BuiltinCarriers {
    private BuiltinCarriers() {}
    public static void register() {
        add("ars_nouveau:glyph_projectile", false, false, false, null);
        add("ars_nouveau:glyph_orbit", true, true, true, null);
        add("ars_elemental:glyph_arc_projectile", true, false, false, null);
        add("ars_elemental:glyph_homing_projectile", true, false, false, null);
        add("arsomega:glyph_missile", true, false, true, null);
        add("not_enough_glyphs:glyph_trail", true, true, true, null);
        add("arsomega:glyph_propagate_projectile", true, false, false, "ars_nouveau:glyph_projectile");
        add("ars_elemental:glyph_propagator_arc", true, false, false, "ars_elemental:glyph_arc_projectile");
        add("ars_elemental:glyph_propagator_homing", true, false, false, "ars_elemental:glyph_homing_projectile");
        add("arsomega:glyph_propagate_missile", true, false, true, "arsomega:glyph_missile");
    }
    private static void add(String id, boolean driven, boolean repeated, boolean area, String emitted) {
        CarrierRegistry.register(new CarrierProfile(CarrierRegistry.id(id), driven, repeated,
                emitted == null ? null : CarrierRegistry.id(emitted), () -> augments(driven, area, emitted != null)));
    }
    private static Set<AbstractAugment> augments(boolean driven, boolean area, boolean propagate) {
        var result = new HashSet<AbstractAugment>(Set.of(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE,
                AugmentAccelerate.INSTANCE, AugmentDecelerate.INSTANCE, AugmentSplit.INSTANCE));
        if (driven) result.addAll(Set.of(AugmentPierce.INSTANCE, AugmentSensitive.INSTANCE));
        if (area) result.addAll(Set.of(AugmentAOE.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE));
        if (propagate) result.add(AugmentExtract.INSTANCE);
        return Set.copyOf(result);
    }
}
