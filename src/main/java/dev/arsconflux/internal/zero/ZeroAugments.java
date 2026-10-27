package dev.arsconflux.internal.zero;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import dev.arsconflux.api.glyph.AugmentEquivalence;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.config.ConfluxStartupConfig;

/** Register Zero's actual equivalents without enabling its optional glyphs or linking their classes. */
public final class ZeroAugments {
    private ZeroAugments() {}
    public static void register() {
        if(!ConfluxStartupConfig.zero() || !ZeroMixinSupport.supported())return;
        for(var definition:ZeroAugmentWeights.definitions()) {
            var id=CarrierRegistry.id("ars_zero:"+definition.glyph());
            var part=GlyphRegistry.getSpellPart(id);
            if(part instanceof AbstractAugment && part.getClass().getName().startsWith("com.github.ars_zero.common.glyph.augment."))
                AugmentEquivalence.register(id,CarrierRegistry.id("ars_nouveau:"+definition.base()),definition.units());
        }
    }
}
