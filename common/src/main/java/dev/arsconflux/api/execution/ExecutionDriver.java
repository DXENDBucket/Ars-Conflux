package dev.arsconflux.api.execution;

import net.minecraft.world.phys.Vec3;

/** Concrete behavior belongs to an addon. P is its own immutable parameter snapshot. */
public interface ExecutionDriver<P> {
    boolean start(ExecutionSession<P> session);
    boolean tick(ExecutionSession<P> session);
    void close(ExecutionSession<P> session, ExecutionSession.EndReason reason);
    default Vec3 incomingDirection() { return Vec3.ZERO; }
    default boolean occupiesCaster() { return false; }
}
