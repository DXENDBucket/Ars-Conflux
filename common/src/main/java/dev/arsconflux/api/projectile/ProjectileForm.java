package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.resource.ResourceCharge;
import java.util.List;

/** Implement on an Ars augment that replaces a carrier's payload. One payload per phrase.
 * Native entity/payload code stays in the addon. True consumes the original spawn,
 * including denied/unaffordable attempts; false requests the unchanged Ars spawn. */
public interface ProjectileForm extends TriggerPaidGlyph {
    boolean replace(EntityProjectileSpell projectile, CarrierProfile carrier);
    ResourceCharge initialCharge(SpellContext context, List<AbstractAugment> augments);
}
