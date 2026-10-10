package dev.arsconflux.api.execution;

import dev.arsconflux.api.resource.ResourceCharge;
import java.util.function.BooleanSupplier;

@FunctionalInterface
public interface TriggerPayment {
    TriggerPayment FREE = BooleanSupplier::getAsBoolean;
    boolean trigger(BooleanSupplier action);
    static TriggerPayment of(ResourceCharge charge) { return charge::trigger; }
}
