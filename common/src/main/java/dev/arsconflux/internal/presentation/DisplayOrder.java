package dev.arsconflux.internal.presentation;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Keep categories contiguous even when addons share numeric sort indices. */
public final class DisplayOrder {
    private DisplayOrder() {}
    public static <T, K> Comparator<T> categories(List<T> values, Comparator<T> original,
            Function<T, K> category, ToIntFunction<T> rank, ToIntFunction<K> categoryOrder) {
        var ranks = new HashMap<K, Integer>();
        for (var value : values) ranks.merge(category.apply(value), rank.applyAsInt(value), Math::min);
        return Comparator.<T>comparingInt(value -> ranks.get(category.apply(value)))
                .thenComparingInt(value -> categoryOrder.applyAsInt(category.apply(value)))
                .thenComparing(original);
    }
}
