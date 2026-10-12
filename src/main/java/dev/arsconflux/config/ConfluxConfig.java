package dev.arsconflux.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConfluxConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue VANILLA_TRIGGER_BILLING;
    public static final ModConfigSpec.BooleanValue HEX_CHANTING, HEX_COOLDOWNS;
    public static final ModConfigSpec.BooleanValue PRESERVE_COOLDOWNS_ON_DEATH;
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
        builder.pop();
        builder.push("hex_linker");
        HEX_CHANTING = builder.comment("Apply installed providers' chanting to new active Hex-Ars Linker operations.",
                "Passive impact callbacks never initiate another chant. Provider settings still apply.")
                .define("chanting", true);
        HEX_COOLDOWNS = builder.comment("Check and commit installed providers' cooldowns for active Hex-Ars Linker operations.",
                "False bypasses cooldowns only for this spell chain; mana and media billing are unchanged.")
                .define("cooldowns", true);
        builder.pop();
        builder.push("addon_cooldowns");
        PRESERVE_COOLDOWNS_ON_DEATH = builder.comment("Keep opt-in addon cooldown deadlines after player death.",
                "Non-death clones always keep deadlines. Native Iron/Goety stores are not changed.")
                .define("preserve_on_death", true);
        builder.pop(); SPEC = builder.build();
    }
    private ConfluxConfig() {}
    public static boolean preserveCooldownsOnDeath() { return SPEC.isLoaded() ? PRESERVE_COOLDOWNS_ON_DEATH.get() : PRESERVE_COOLDOWNS_ON_DEATH.getDefault(); }
    public static boolean hexChanting() { return SPEC.isLoaded() ? HEX_CHANTING.get() : HEX_CHANTING.getDefault(); }
    public static boolean hexCooldowns() { return SPEC.isLoaded() ? HEX_COOLDOWNS.get() : HEX_COOLDOWNS.getDefault(); }
    public static boolean sumChants() { return (SPEC.isLoaded() ? CHANT_MODE.get() : CHANT_MODE.getDefault()) == ChantMode.SUM; }
    public static boolean vanillaTriggerBilling() {
        return SPEC.isLoaded() ? VANILLA_TRIGGER_BILLING.get() : VANILLA_TRIGGER_BILLING.getDefault();
    }
}
