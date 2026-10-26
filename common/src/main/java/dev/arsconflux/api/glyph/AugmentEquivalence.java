package dev.arsconflux.api.glyph;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Semantic equivalents for provider rules that cannot consume SpellStats directly.
 * Registration is startup-only. This never rewrites a recipe, its price or Ars spell limits.
 * Numeric effects should still read SpellStats, where each augment applies its own modifiers. */
public final class AugmentEquivalence {
    private record Equivalent(ResourceLocation base, double units) {}
    private static final Map<ResourceLocation, Equivalent> EQUIVALENTS = new LinkedHashMap<>();
    private AugmentEquivalence() {}

    /** A negative weight is useful for a reduced-area augment equivalent to negative AOE.
     * Bases must be canonical glyphs, not another registered equivalent. */
    public static void register(ResourceLocation augment, ResourceLocation base, double units) {
        Objects.requireNonNull(augment); Objects.requireNonNull(base);
        if (augment.equals(base) || !Double.isFinite(units) || units == 0 || EQUIVALENTS.containsKey(base)
                || EQUIVALENTS.values().stream().anyMatch(value -> value.base().equals(augment)))
            throw new IllegalArgumentException("Invalid augment equivalence " + augment + " -> " + base);
        var value = new Equivalent(base, units);
        var previous = EQUIVALENTS.putIfAbsent(augment, value);
        if (previous != null && !previous.equals(value))
            throw new IllegalArgumentException("Conflicting augment equivalence " + augment);
    }
    public static double units(AbstractAugment augment, AbstractAugment base) {
        if (augment == null || base == null) return 0;
        if (augment == base) return 1;
        var value = EQUIVALENTS.get(augment.getRegistryName());
        return value != null && value.base().equals(base.getRegistryName()) ? value.units() : 0;
    }
    public static boolean matches(AbstractAugment augment, AbstractAugment base) {
        return units(augment, base) != 0;
    }
    public static double sum(Collection<? extends AbstractAugment> augments, AbstractAugment base) {
        return augments.stream().mapToDouble(augment -> units(augment, base)).sum();
    }
    public static boolean supports(Set<AbstractAugment> allowed, AbstractAugment candidate) {
        return allowed.contains(candidate) || allowed.stream().anyMatch(base -> matches(candidate, base));
    }
    /** A snapshot suitable for Ars' own compatibleAugments set and palette validation. */
    public static Set<AbstractAugment> expand(Set<AbstractAugment> allowed) {
        var result = new HashSet<>(allowed);
        for (var id : EQUIVALENTS.keySet())
            if (GlyphRegistry.getSpellPart(id) instanceof AbstractAugment augment && supports(allowed, augment))
                result.add(augment);
        return Set.copyOf(result);
    }
    /** Apply registered equivalents after providers have populated their base eligibility sets. */
    public static void refreshCompatibility() {
        for (var part : GlyphRegistry.getSpellpartMap().values())
            part.compatibleAugments.addAll(expand(part.compatibleAugments));
    }
}
