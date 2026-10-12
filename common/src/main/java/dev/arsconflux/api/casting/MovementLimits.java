package dev.arsconflux.api.casting;

import java.util.Map;
import java.util.WeakHashMap;

/** Client input-event composition, with no loader, input type or provider-specific attributes.
 * Apply the returned multiplier to both input axes. Participants must share the event object.
 * Weak event keys expire after the update; no entity state or cross-frame lease is retained. */
public final class MovementLimits {
    private static final Map<Object, Float> APPLIED = new WeakHashMap<>();
    private MovementLimits() {}
    public static float multiplier(Object inputEvent, float requested) {
        if (!Float.isFinite(requested)) return 1;
        requested = Math.max(0, requested);
        var previous = APPLIED.get(inputEvent);
        float combined = previous == null ? requested : Math.min(previous, requested);
        APPLIED.put(inputEvent, combined);
        return previous == null ? combined : previous == 0 ? 1 : combined / previous;
    }
}
