package dev.arsconflux.api.resource;

/** One physical resource pool. Wrappers for the same pool MUST return equal reservation keys.
 * A key includes the resource kind, so one actor's mana and soul pools remain independent.
 * Methods are synchronous and server-thread-only; amounts use the provider's integer units. */
public interface ResourceAccount {
    Object reservationKey();
    boolean canSpend(int amount);
    void spend(int amount);
    default boolean free() { return false; }
}
