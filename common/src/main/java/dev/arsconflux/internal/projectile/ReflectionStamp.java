package dev.arsconflux.internal.projectile;

/** Attached to Ars carriers; a synchronous reflection does not consume the intercepted hit. */
public interface ReflectionStamp {
    long conflux$reflectionVersion();
    void conflux$markReflection();
}
