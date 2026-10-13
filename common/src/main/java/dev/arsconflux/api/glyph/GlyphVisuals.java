package dev.arsconflux.api.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Server-safe icon declarations; client rendering has no native-mod dependencies. */
public final class GlyphVisuals {
    private static final Map<String, Function<AbstractSpellPart, GlyphIcon>> PROVIDERS = new ConcurrentHashMap<>();

    public static void register(String namespace, Function<AbstractSpellPart, GlyphIcon> provider) {
        if (namespace == null || namespace.isBlank()) throw new IllegalArgumentException("Empty icon namespace");
        if (PROVIDERS.putIfAbsent(namespace, Objects.requireNonNull(provider)) != null)
            throw new IllegalStateException("Duplicate icon provider " + namespace);
    }
    public static boolean applies(AbstractSpellPart part) {
        String namespace = part.getRegistryName().getNamespace();
        return PROVIDERS.containsKey(namespace) || GlyphPresentation.isProvider(namespace);
    }
    /** Null keeps the glyph's existing item model, including its native custom renderer. */
    public static GlyphIcon icon(AbstractSpellPart part) {
        var provider = PROVIDERS.get(part.getRegistryName().getNamespace());
        return provider == null ? null : provider.apply(part);
    }
    private GlyphVisuals() {}
}
