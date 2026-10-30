package dev.arsconflux.internal.billing;

import dev.arsconflux.config.ConfluxStartupConfig;
import dev.arsconflux.mixin.RangedAttributeAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** Relax native Ars numeric ceilings once at startup; base values and modifiers stay untouched. */
public final class ManaAttributeLimits {
    public static void apply(Attribute maximumMana,Attribute regeneration) {
        if(!ConfluxStartupConfig.manaAttributeLimits())return;
        widen(maximumMana,ConfluxStartupConfig.maximumManaAttribute());
        widen(regeneration,ConfluxStartupConfig.maximumManaRegenerationAttribute());
    }
    private static void widen(Attribute attribute,double requested) {
        if(attribute instanceof RangedAttribute ranged&&ranged.getMaxValue()<requested)
            ((RangedAttributeAccess)ranged).arsConflux$maximum(requested);
    }
    private ManaAttributeLimits() { }
}
