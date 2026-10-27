package dev.arsconflux.api.casting;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Additional preparation lifetime. A wearable need not retain an unrelated hand item. */
public record ActiveCastGuard(boolean retainHandItem, BooleanSupplier valid) {
    public static final ActiveCastGuard DEFAULT = new ActiveCastGuard(true, () -> true);
    public ActiveCastGuard { Objects.requireNonNull(valid); }
}
