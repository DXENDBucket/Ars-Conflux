package dev.arsconflux.api.resource;

import java.util.Objects;

/** Shared identity for adapters which access the same physical resource pool.
 * Use a resource kind, not the paying addon's ID. Linked pools must use one canonical key. */
public record ResourcePoolKey(String kind, Object owner) {
    public ResourcePoolKey {
        Objects.requireNonNull(kind, "Resource kind");
        if (!kind.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Expected a namespaced resource kind: " + kind);
        Objects.requireNonNull(owner, "Pool owner");
    }
    public static ResourcePoolKey of(String kind, Object owner) {
        return new ResourcePoolKey(kind, owner);
    }
}
