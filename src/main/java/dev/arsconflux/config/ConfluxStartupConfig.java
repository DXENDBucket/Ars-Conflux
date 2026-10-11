package dev.arsconflux.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Registry-affecting optional content must be configured before addon constructors run. */
public final class ConfluxStartupConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ELEMENTAL_PHALANX;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("optional_content");
        ELEMENTAL_PHALANX = builder.comment(
                "Register Ars Elemental's Carian Phalanx in normal games when its implementation is present.",
                "Also adds an Ars Scribe's Table recipe. Uses Elemental's original glyph, texture and mechanics.",
                "False preserves Elemental's own registration policy. Requires a restart; match client and server.")
                .define("enable_elemental_phalanx", true);
        builder.pop(); SPEC = builder.build();
    }
    private ConfluxStartupConfig() {}
    public static boolean elementalPhalanx() {
        return SPEC.isLoaded() ? ELEMENTAL_PHALANX.get() : ELEMENTAL_PHALANX.getDefault();
    }
}
