package dev.arsconflux.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ConfluxConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue VANILLA_TRIGGER_BILLING;
    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("ars_trigger_billing");
        VANILLA_TRIGGER_BILLING = builder.comment(
                "Move native Ars effects and their written augments from launch cost to each effect dispatch.",
                "Includes Orbit creation at every nesting level; insufficient mana stops that branch.",
                "Cast methods and addon glyphs retain their upfront costs. Creative casts remain free.",
                "Living casters only; source-powered devices keep their native payment rules.",
                "A normally returning effect is a paid attempt, even if its target is inapplicable.",
                "False preserves native Ars billing.")
                .define("enabled", false);
        builder.pop(); SPEC = builder.build();
    }
    private ConfluxConfig() {}
    public static boolean vanillaTriggerBilling() {
        return SPEC.isLoaded() ? VANILLA_TRIGGER_BILLING.get() : VANILLA_TRIGGER_BILLING.getDefault();
    }
}
