package dev.arsconflux.api.execution;

import java.util.function.BooleanSupplier;

/** Restore addon-specific authorization/context around every deferred callback. */
@FunctionalInterface
public interface ExecutionScope {
    ExecutionScope DIRECT = Runnable::run;
    void run(Runnable action);
    default boolean call(BooleanSupplier action) {
        boolean[] result = new boolean[1];
        run(() -> result[0] = action.getAsBoolean());
        return result[0];
    }
}
