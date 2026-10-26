package dev.arsconflux.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Registry-affecting optional content must be configured before addon constructors run. */
public final class ConfluxStartupConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ELEMENTAL_PHALANX, NEARBY_ALLIES, AUGMENT_EXTENSIONS;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("optional_content");
        ELEMENTAL_PHALANX = builder.comment(
                "Register Ars Elemental's Carian Phalanx in normal games when its implementation is present.",
                "Also adds an Ars Scribe's Table recipe. Uses Elemental's original glyph, texture and mechanics.",
                "False preserves Elemental's own registration policy. Requires a restart; match client and server.")
                .define("enable_elemental_phalanx", false);
        NEARBY_ALLIES=builder.comment("Register the nonstandard Nearby Allies cast method and Scribe recipe. Requires restart; match client and server.").define("enable_nearby_allies", false);
        builder.pop();
        builder.push("compatibility");
        AUGMENT_EXTENSIONS=builder.comment("Enable optional AAP/NEG augment extensions and weighted Controle delays. Requires restart; match client and server.").define("enable_augment_extensions",true);
        builder.pop(); SPEC = builder.build();
    }
    public static boolean nearbyAllies() {return SPEC.isLoaded()?NEARBY_ALLIES.get():NEARBY_ALLIES.getDefault();}
    public static boolean augmentExtensions() { return SPEC.isLoaded()?AUGMENT_EXTENSIONS.get():AUGMENT_EXTENSIONS.getDefault(); }
    private ConfluxStartupConfig() {}
    public static boolean elementalPhalanx() {
        return SPEC.isLoaded() ? ELEMENTAL_PHALANX.get() : ELEMENTAL_PHALANX.getDefault();
    }
}
