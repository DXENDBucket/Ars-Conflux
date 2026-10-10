package dev.arsconflux.api.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import dev.arsconflux.api.resource.ResourceCharge;
import java.util.List;

/** Opt in to Ars upfront payment ordering, before trigger-paid branches execute.
 * This marker does not alter learning, tier, mana cost, damage or native spell levels. */
public interface TriggerPaidGlyph {
    /** Dry first-trigger estimate. Null means unknown, never free. Does not predict later repetitions. */
    default ResourceCharge firstCharge(SpellContext context, List<AbstractAugment> augments) { return null; }
}
