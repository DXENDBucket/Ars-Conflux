package dev.arsconflux.api.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Cross-addon reservations share the physical pool, not a glyph's namespace or wrapper identity.
 * A failed action is uncharged. A successful nested action is not rolled back if its parent fails. */
public final class ResourceTransactions {
    private static final ThreadLocal<Map<Object, Long>> RESERVED = ThreadLocal.withInitial(HashMap::new);
    private ResourceTransactions() {}

    public static boolean canSpend(ResourceAccount account, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative resource request");
        if (account.free()) return true;
        long held = RESERVED.get().getOrDefault(account.reservationKey(), 0L);
        return amount <= Integer.MAX_VALUE - held && account.canSpend((int)(held + amount));
    }
    public static int affordableCount(ResourceCharge charge, int requested) {
        if (requested < 0) throw new IllegalArgumentException("Negative trigger count");
        if (charge.amount() == 0 || charge.account().free()) return requested;
        int low = 0, high = requested;
        while (low < high) {
            int mid = low + (int)(((long)high - low + 1) / 2);
            if (canSpend(charge.account(), (long)mid * charge.amount())) low = mid; else high = mid - 1;
        }
        return low;
    }
    public static boolean trigger(ResourceCharge charge, BooleanSupplier action) {
        var account = charge.account();
        if (account.free()) return action.getAsBoolean();
        var reservations = RESERVED.get();
        Object key = Objects.requireNonNull(account.reservationKey());
        long held = reservations.getOrDefault(key, 0L), needed = held + charge.amount();
        if (needed > Integer.MAX_VALUE || !account.canSpend((int)needed)) return false;
        reservations.put(key, needed);
        try {
            boolean applied = action.getAsBoolean();
            if (applied && charge.amount() > 0) account.spend(charge.amount());
            return applied;
        } finally {
            if (held == 0) reservations.remove(key); else reservations.put(key, held);
            if (reservations.isEmpty()) RESERVED.remove();
        }
    }
}
