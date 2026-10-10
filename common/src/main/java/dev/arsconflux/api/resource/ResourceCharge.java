package dev.arsconflux.api.resource;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** A provider computes its own price; Core makes no assumption about spell levels or power. */
public record ResourceCharge(ResourceAccount account, int amount) {
    public ResourceCharge {
        Objects.requireNonNull(account);
        if (amount < 0) throw new IllegalArgumentException("Negative resource charge");
    }
    public boolean trigger(BooleanSupplier action) { return ResourceTransactions.trigger(this, action); }
    public int affordableCount(int requested) { return ResourceTransactions.affordableCount(this, requested); }
}
