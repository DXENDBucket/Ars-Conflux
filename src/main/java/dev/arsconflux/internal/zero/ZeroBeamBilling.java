package dev.arsconflux.internal.zero;

import com.github.ars_zero.common.glyph.EffectBeam;
import com.hollingsworth.arsnouveau.api.spell.*;

/** Zero retains its own beam price; only duplicate prices owned by dispatch are omitted. */
public final class ZeroBeamBilling {
    private ZeroBeamBilling() {}
    public static double forwardCost(SpellContext context) {
        return ZeroForwardBilling.price(context,EffectBeam.INSTANCE.getResolverManaCostMultiplier());
    }
}
