package dev.arsconflux.internal.zero;

import com.github.ars_zero.common.glyph.EffectBeam;
import com.hollingsworth.arsnouveau.api.spell.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.internal.billing.VanillaTriggerBilling;
import java.util.ArrayList;

/** Zero retains its own beam price; only duplicate prices owned by dispatch are omitted. */
public final class ZeroBeamBilling {
    private ZeroBeamBilling() {}
    public static double forwardCost(SpellContext context) {
        var parts=ArsSpellAccess.parts(context.getSpell());
        var nativePriced=new ArrayList<AbstractSpellPart>(); boolean hasEffect=false;
        for(int i=0;i<parts.size();) {
            var part=parts.get(i++);
            if(part instanceof AbstractAugment)continue;
            boolean managed=VanillaTriggerBilling.ownsDispatch(context,part);
            if(!managed) { nativePriced.add(part); hasEffect |= part instanceof AbstractEffect; }
            while(i<parts.size() && parts.get(i) instanceof AbstractAugment) {
                if(!managed)nativePriced.add(parts.get(i));
                i++;
            }
        }
        return hasEffect ? Math.max(0,new Spell(nativePriced).getCost())*EffectBeam.INSTANCE.getResolverManaCostMultiplier() : 0;
    }
}
