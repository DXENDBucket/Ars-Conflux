package dev.arsconflux.api.casting;
import java.util.Collection;
/** Saturating tick arithmetic, shared by provider groups and the mixed active-cast plan. */
public final class ChantDurations {
    private ChantDurations() {}
    public static int combine(Collection<Integer> ticks, boolean sum) {
        long result = 0;
        for (int tick : ticks) { int safe = Math.max(0, tick); result = sum ? result + safe : Math.max(result, safe); result = Math.min(Integer.MAX_VALUE, result); }
        return (int)result;
    }
}
