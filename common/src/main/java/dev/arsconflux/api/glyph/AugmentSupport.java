package dev.arsconflux.api.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import java.util.HashSet;
import java.util.Set;

/** Opt-in eligibility helper. Callers must handle signed stats and clamp final results themselves. */
public final class AugmentSupport {
    private AugmentSupport() {}
    public static Set<AbstractAugment> withReductions(Set<AbstractAugment> supported) {
        var result = new HashSet<>(supported);
        if (result.contains(AugmentAmplify.INSTANCE)) result.add(AugmentDampen.INSTANCE);
        if (result.contains(AugmentExtendTime.INSTANCE)) result.add(AugmentDurationDown.INSTANCE);
        return Set.copyOf(result);
    }
}
