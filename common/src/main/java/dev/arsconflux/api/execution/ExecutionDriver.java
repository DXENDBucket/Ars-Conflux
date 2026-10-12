package dev.arsconflux.api.execution;

import net.minecraft.world.phys.Vec3;

/** Concrete behavior belongs to an addon. P is its own immutable parameter snapshot. */
public interface ExecutionDriver<P> {
    boolean start(ExecutionSession<P> session);
    boolean tick(ExecutionSession<P> session);
    void close(ExecutionSession<P> session, ExecutionSession.EndReason reason);
    default Vec3 incomingDirection() { return Vec3.ZERO; }
    default boolean occupiesCaster() { return false; }
    /** Explicit actor claims supersede the legacy caster flag. No claim is needed for a fixed point. */
    default java.util.List<ExecutionClaim> claims(ExecutionSession<P> session) {
        if(!occupiesCaster())return java.util.List.of();
        var actor=session.context().executor();
        return java.util.List.of(ExecutionClaim.action(actor==null?session.caster():actor));
    }
}
