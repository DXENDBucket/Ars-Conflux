package dev.arsconflux.api.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import dev.arsconflux.internal.ability.AbilityData;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Per-glyph hints. Server data overrides complete sets; an explicit empty set masks defaults. */
public final class GlyphAbilities {
    private static final Map<ResourceLocation,Set<ResourceLocation>> DEFAULTS=new ConcurrentHashMap<>();
    public static void declare(ResourceLocation glyph,Set<ResourceLocation> tags) {
        Objects.requireNonNull(glyph);
        var value=Set.copyOf(tags);
        var old=DEFAULTS.putIfAbsent(glyph,value);
        if(old!=null&&!old.equals(value))throw new IllegalArgumentException("Conflicting glyph abilities: "+glyph);
    }
    public static Set<ResourceLocation> get(ResourceLocation glyph) {
        return glyph==null?Set.of():AbilityData.current().getOrDefault(glyph,DEFAULTS.getOrDefault(glyph,Set.of()));
    }
    public static Set<ResourceLocation> get(AbstractSpellPart glyph) {return get(glyph==null?null:glyph.getRegistryName());}
    public static boolean has(AbstractSpellPart glyph,ResourceLocation tag) {return get(glyph).contains(tag);}
    /** Includes explicitly empty declarations; absence is not an assertion about a spell. */
    public static Map<ResourceLocation,Set<ResourceLocation>> declarations() {
        var result=new TreeMap<ResourceLocation,Set<ResourceLocation>>(DEFAULTS);
        result.putAll(AbilityData.current());return Collections.unmodifiableMap(result);
    }
    private GlyphAbilities() {}
}
