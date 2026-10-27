package dev.arsconflux.internal.zero;

import com.hollingsworth.arsnouveau.api.spell.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.internal.billing.*;

/** Shared by Beam and Geometrize. The caller's own glyph/base price remains native. */
public final class ZeroForwardBilling {
    private ZeroForwardBilling() {}
    public static double price(SpellContext context,double multiplier) {
        var remaining=ArsSpellAccess.parts(context.getRemainingSpell());
        var nativeParts=ForwardedGroups.unmanaged(remaining,AbstractAugment.class::isInstance,
                part->VanillaTriggerBilling.ownsDispatch(context,part),AbstractEffect.class::isInstance);
        return nativeParts.isEmpty()?0:Math.max(0,new Spell(nativeParts).getCost())*Math.max(0,multiplier);
    }
}
