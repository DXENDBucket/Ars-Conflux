package dev.arsconflux.internal.zero;

/** Implemented only on the optional native per-use context. No global player target cache. */
public interface ZeroTemporalOwner {
    ZeroTemporalState arsConflux$temporal();
}
