package dev.arsconflux.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConfluxConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue VANILLA_TRIGGER_BILLING;
    public enum ChantMode { MAXIMUM, SUM }
    public static final ModConfigSpec.EnumValue<ChantMode> CHANT_MODE;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("ars_trigger_billing");
        VANILLA_TRIGGER_BILLING = builder.comment(
                "Move native Ars effects and their written augments from launch cost to each effect dispatch.",
                "Includes Orbit creation at every nesting level; insufficient mana stops that branch.",
                "Cast methods and addon glyphs retain their upfront costs. Creative casts remain free.",
                "Living casters only; source-powered devices keep their native payment rules.",
                "A normally returning effect is a paid attempt, even if its target is inapplicable.",
                "False preserves native Ars billing.")
                .define("enabled", false);
        builder.pop();
        builder.push("active_casting");
        CHANT_MODE = builder.comment("Aggregation for preparation times contributed by all installed providers.",
                "MAXIMUM: longest effective preparation. SUM: add every written occurrence across providers.",
                "Providers independently decide whether they contribute chanting and native cooldowns.")
                .defineEnum("chant_mode", ChantMode.MAXIMUM);
        builder.pop(); SPEC = builder.build();
    }
    private ConfluxConfig() {}
    public static boolean sumChants() { return (SPEC.isLoaded() ? CHANT_MODE.get() : CHANT_MODE.getDefault()) == ChantMode.SUM; }
    public static boolean vanillaTriggerBilling() {
        return SPEC.isLoaded() ? VANILLA_TRIGGER_BILLING.get() : VANILLA_TRIGGER_BILLING.getDefault();
    }
}
