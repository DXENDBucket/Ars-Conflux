package dev.arsconflux.glyph;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import dev.arsconflux.config.ConfluxStartupConfig;
public final class OptionalGlyphs {
    private OptionalGlyphs() {}
    public static void register() {
        dev.arsconflux.api.glyph.GlyphVisuals.register("ars_conflux", part -> null);
        if(ConfluxStartupConfig.nearbyAllies()) GlyphRegistry.registerSpell(MethodNearbyAllies.INSTANCE);
        if(dev.arsconflux.platform.ArsAccess.modLoaded("not_enough_glyphs"))dev.arsconflux.internal.neg.NegFormGlyphs.register();
    }
}
