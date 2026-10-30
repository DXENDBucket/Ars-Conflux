package dev.arsconflux.api.school;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import dev.arsconflux.internal.school.SchoolData;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Optional code defaults plus server datapack overrides, including explicit empty overrides. */
public final class GlyphSchools {
    private static final Map<ResourceLocation, SchoolWeights> DEFAULTS = new ConcurrentHashMap<>();
    public static void declare(ResourceLocation glyph, SchoolWeights weights) {
        Objects.requireNonNull(glyph); Objects.requireNonNull(weights);
        var old = DEFAULTS.putIfAbsent(glyph, weights);
        if (old != null && !old.equals(weights)) throw new IllegalArgumentException("Glyph schools already declared: " + glyph);
    }
    public static SchoolWeights get(ResourceLocation glyph) {
        if (glyph == null) return SchoolWeights.EMPTY;
        return SchoolData.current().glyphs().getOrDefault(glyph, DEFAULTS.getOrDefault(glyph, SchoolWeights.EMPTY));
    }
    public static SchoolWeights get(AbstractSpellPart glyph) { return glyph == null ? SchoolWeights.EMPTY : get(glyph.getRegistryName()); }
    public static Set<ResourceLocation> declaredGlyphs() {
        var ids = new TreeSet<>(DEFAULTS.keySet()); ids.addAll(SchoolData.current().glyphs().keySet()); return Collections.unmodifiableSet(ids);
    }
    private GlyphSchools() {}
}
