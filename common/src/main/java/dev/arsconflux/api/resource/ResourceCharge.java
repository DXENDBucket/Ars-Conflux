package dev.arsconflux.api.resource;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** A provider computes its own price; Core makes no assumption about spell levels or power. */
public record ResourceCharge(ResourceAccount account, int amount, int originalAmount) {
    /** Existing ABI: construct from a raw provider cost and freeze its account's current quote. */
    public ResourceCharge(ResourceAccount account,int raw) {this(account,Objects.requireNonNull(account).quoteCost(raw),raw);}
    /** Costs already processed by a native event or another quote must not be discounted twice. */
    public static ResourceCharge exact(ResourceAccount account,int amount) {return new ResourceCharge(account,amount,amount);}
    public ResourceCharge {
        Objects.requireNonNull(account);
        if (originalAmount < 0) throw new IllegalArgumentException("Negative original resource charge");
        if (amount < 0) throw new IllegalArgumentException("Negative resource charge");
    }
    public boolean trigger(BooleanSupplier action) { return ResourceTransactions.trigger(this, action); }
    public int affordableCount(int requested) { return ResourceTransactions.affordableCount(this, requested); }
}
