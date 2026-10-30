package dev.arsconflux.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConfluxConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue PERCENTAGE_DISCOUNT_RINGS;
    public static final ModConfigSpec.DoubleValue LESSER_RING_REDUCTION, GREATER_RING_REDUCTION;
    public static final ModConfigSpec.BooleanValue SCHOOL_DAMAGE_RULES;
    public static final ModConfigSpec.BooleanValue VANILLA_TRIGGER_BILLING;
    public static final ModConfigSpec.BooleanValue HEX_CHANTING, HEX_COOLDOWNS;
    public static final ModConfigSpec.BooleanValue PRESERVE_COOLDOWNS_ON_DEATH;
    public static final ModConfigSpec.BooleanValue ZERO_CHANTING, ZERO_COOLDOWNS;
    public enum ChantMode { MAXIMUM, SUM }
    public static final ModConfigSpec.EnumValue<ChantMode> CHANT_MODE;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("mana_discounts");
        PERCENTAGE_DISCOUNT_RINGS=builder.comment("Convert the two native Ars discount rings from flat discounts to percentage mana reductions.",
                "Applies to native Ars costs and opt-in mana quotes. Soul energy and other resources keep their own rules.",
                "False restores native flat discounts and removes the converted trigger discounts.").define("percentage_discount_rings",true);
        LESSER_RING_REDUCTION=builder.comment("Mana reduction per equipped lesser discount ring; 0.10 means 10 percent.").defineInRange("lesser_ring_reduction",0.10,0,1);
        GREATER_RING_REDUCTION=builder.comment("Mana reduction per equipped greater discount ring; 0.20 means 20 percent.",
                "Ring percentages add, capped at 100 percent. Positive fractional mana costs round up.").defineInRange("greater_ring_reduction",0.20,0,1);
        builder.pop();
        builder.push("schools");
        SCHOOL_DAMAGE_RULES=builder.comment("Apply explicitly declared school damage multipliers from server datapacks. Default false preserves existing balance.",
                "Undeclared abilities always retain their original damage. Native damage types, armor rules and original school memberships are unchanged.").define("damage_rules",false);
        builder.pop();
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
        builder.push("zero");
        ZERO_CHANTING = builder.comment("Prepare all phases of one Zero staff/circlet use once using installed providers' chanting.").define("chanting", true);
        ZERO_COOLDOWNS = builder.comment("Check the complete Zero phase plan at activation and commit its provider cooldowns once at the first acknowledged release.").define("cooldowns", true);
        builder.pop();
        builder.push("addon_cooldowns");
        PRESERVE_COOLDOWNS_ON_DEATH = builder.comment("Keep opt-in addon cooldown deadlines after player death.",
                "Non-death clones always keep deadlines. Native Iron/Goety stores are not changed.")
                .define("preserve_on_death", true);
        builder.pop(); SPEC = builder.build();
    }
    public static boolean percentageDiscountRings() {return SPEC.isLoaded()?PERCENTAGE_DISCOUNT_RINGS.get():PERCENTAGE_DISCOUNT_RINGS.getDefault();}
    public static double lesserRingReduction() {return SPEC.isLoaded()?LESSER_RING_REDUCTION.get():LESSER_RING_REDUCTION.getDefault();}
    public static double greaterRingReduction() {return SPEC.isLoaded()?GREATER_RING_REDUCTION.get():GREATER_RING_REDUCTION.getDefault();}
    private ConfluxConfig() {}
    public static boolean schoolDamageRules() {return SPEC.isLoaded()?SCHOOL_DAMAGE_RULES.get():SCHOOL_DAMAGE_RULES.getDefault();}
    public static boolean preserveCooldownsOnDeath() { return SPEC.isLoaded() ? PRESERVE_COOLDOWNS_ON_DEATH.get() : PRESERVE_COOLDOWNS_ON_DEATH.getDefault(); }
    public static boolean hexChanting() { return SPEC.isLoaded() ? HEX_CHANTING.get() : HEX_CHANTING.getDefault(); }
    public static boolean hexCooldowns() { return SPEC.isLoaded() ? HEX_COOLDOWNS.get() : HEX_COOLDOWNS.getDefault(); }
    public static boolean sumChants() { return (SPEC.isLoaded() ? CHANT_MODE.get() : CHANT_MODE.getDefault()) == ChantMode.SUM; }
    public static boolean vanillaTriggerBilling() {
        return SPEC.isLoaded() ? VANILLA_TRIGGER_BILLING.get() : VANILLA_TRIGGER_BILLING.getDefault();
    }
    public static boolean zeroChanting() { return SPEC.isLoaded()?ZERO_CHANTING.get():ZERO_CHANTING.getDefault(); }
    public static boolean zeroCooldowns() { return SPEC.isLoaded()?ZERO_COOLDOWNS.get():ZERO_COOLDOWNS.getDefault(); }
}
