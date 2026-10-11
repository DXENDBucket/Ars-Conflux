package dev.arsconflux.glyph;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import dev.arsconflux.config.ConfluxStartupConfig;
public final class OptionalGlyphs {
    private OptionalGlyphs() {}
    public static void register() {
        if(ConfluxStartupConfig.nearbyAllies()) GlyphRegistry.registerSpell(MethodNearbyAllies.INSTANCE);
    }
}
