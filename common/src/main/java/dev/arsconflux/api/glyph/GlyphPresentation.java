package dev.arsconflux.api.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import dev.arsconflux.internal.presentation.DisplayOrder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Presentation only: retain semantic categories and group opt-in providers after ordinary glyphs. */
public final class GlyphPresentation {
    private static final Map<String, Function<AbstractSpellPart, String>> PROVIDERS = new ConcurrentHashMap<>();
    private GlyphPresentation() {}

    /** Register during mod initialization. Namespace determines stable provider order, not load order.
     * The secondary key orders a provider's own glyphs (for example its native magic schools). */
    public static void register(String namespace, Function<AbstractSpellPart, String> secondaryKey) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(secondaryKey);
        if (namespace.isBlank()) throw new IllegalArgumentException("Empty glyph namespace");
        if (PROVIDERS.putIfAbsent(namespace, secondaryKey) != null)
            throw new IllegalStateException("Glyph presentation already registered: " + namespace);
    }
    public static void register(String namespace) { register(namespace, part -> ""); }

    private static String provider(AbstractSpellPart part) {
        var namespace = part.getRegistryName().getNamespace();
        return PROVIDERS.containsKey(namespace) ? namespace : "";
    }
    private static String secondary(AbstractSpellPart part) {
        var key = PROVIDERS.get(part.getRegistryName().getNamespace());
        return key == null ? "" : Objects.requireNonNullElse(key.apply(part), "");
    }
    private static <T> Comparator<T> sourceOrder(Comparator<T> original, Function<T, AbstractSpellPart> part) {
        return Comparator.<T, String>comparing(value -> provider(part.apply(value)))
                .thenComparing(value -> secondary(part.apply(value))).thenComparing(original);
    }
    /** The comparator reads registrations at comparison time: Ars may initialize before addons. */
    public static <T> Comparator<T> afterPeers(Comparator<T> original, Function<T, AbstractSpellPart> part,
                                             ToIntFunction<AbstractSpellPart> category) {
        var grouped = Comparator.<T>comparingInt(value -> category.applyAsInt(part.apply(value)))
                .thenComparing(sourceOrder(original, part));
        return (left, right) -> (PROVIDERS.isEmpty() ? original : grouped).compare(left, right);
    }
    /** Categories come from the actual client predicates, including optional addon specializations. */
    public static <K> Comparator<AbstractSpellPart> withinCategories(List<AbstractSpellPart> values,
            Comparator<AbstractSpellPart> original, Function<AbstractSpellPart, K> category,
            ToIntFunction<AbstractSpellPart> rank, ToIntFunction<K> categoryOrder) {
        if (PROVIDERS.isEmpty()) return original;
        return DisplayOrder.categories(values, sourceOrder(original, Function.identity()), category, rank, categoryOrder);
    }
}
