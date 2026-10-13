package dev.arsconflux.internal.presentation;

import java.util.*;
import java.util.function.Function;

/** Multiset difference: an existing violation must not invalidate an unrelated new glyph. */
public final class PreviewErrors {
    public static <E, K> Set<E> introduced(List<E> current, List<E> previous, Function<E, K> key) {
        var counts = new HashMap<K, Integer>();
        for (var error : previous) counts.merge(key.apply(error), 1, Integer::sum);
        Set<E> result = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var error : current) {
            K value = key.apply(error);
            int count = counts.getOrDefault(value, 0);
            if (count == 0) result.add(error);
            else counts.put(value, count - 1);
        }
        return result;
    }
    private PreviewErrors() {}
}
