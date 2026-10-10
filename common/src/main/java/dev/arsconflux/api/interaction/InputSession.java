package dev.arsconflux.api.interaction;

import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.execution.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** An input endpoint for the existing ExecutionSessions lifecycle, not a second ticking system. */
public final class InputSession<P> {
    private final InputBinding binding;
    private final InteractiveDriver<P> driver;
    private final ExecutionSession<P> execution;
    public InputSession(ServerLevel world, CastContext context, P parameters, int timeout,
                        InputBinding binding, InteractiveDriver<P> driver, ImpactContinuation continuation,
                        BooleanSupplier permission) {
        this(world, context, parameters, timeout, binding, driver, continuation, permission, ExecutionScope.DIRECT);
    }
    public InputSession(ServerLevel world, CastContext context, P parameters, int timeout,
                        InputBinding binding, InteractiveDriver<P> driver, ImpactContinuation continuation,
                        BooleanSupplier permission, ExecutionScope scope) {
        this.binding = Objects.requireNonNull(binding); this.driver = Objects.requireNonNull(driver);
        execution = new ExecutionSession<>(world, context, parameters, timeout, new ExecutionDriver<P>() {
            public boolean start(ExecutionSession<P> session) { return driver.start(session); }
            public boolean tick(ExecutionSession<P> session) {
                if (!binding.valid()) { session.finish(ExecutionSession.EndReason.SOURCE_CHANGED); return false; }
                return driver.tick(session);
            }
            public void close(ExecutionSession<P> session, ExecutionSession.EndReason reason) {
                InputSessions.remove(InputSession.this);
                if (reason == ExecutionSession.EndReason.EXPIRED || reason == ExecutionSession.EndReason.COMPLETED) {
                    if (!binding.valid()) reason = ExecutionSession.EndReason.SOURCE_CHANGED;
                    else if (!session.permitted()) reason = ExecutionSession.EndReason.DENIED;
                }
                driver.close(session, reason);
            }
            public Vec3 incomingDirection() { return driver.incomingDirection(); }
            public boolean occupiesCaster() { return driver.occupiesCaster(); }
        }, continuation, permission, scope);
    }
    public ExecutionSession<P> execution() { return execution; }
    public InputBinding binding() { return binding; }
    public InputResult submit(CastInput input) {
        if (!execution.active()) return InputResult.PASS;
        if (!binding.valid()) { execution.finish(ExecutionSession.EndReason.SOURCE_CHANGED); return InputResult.REJECTED; }
        if (!execution.permitted()) { execution.finish(ExecutionSession.EndReason.DENIED); return InputResult.REJECTED; }
        if (execution.remainingTicks() <= 0) { execution.finish(ExecutionSession.EndReason.EXPIRED); return InputResult.REJECTED; }
        if (input.action() == CastInput.Action.CANCEL) {
            execution.finish(ExecutionSession.EndReason.USER_CANCELLED); return InputResult.ACCEPTED;
        }
        final InputResult[] result = {InputResult.REJECTED};
        try {
            execution.activate(() -> {
                result[0] = Objects.requireNonNull(driver.input(execution, input));
                if (result[0] == InputResult.COMPLETED) execution.finish(ExecutionSession.EndReason.COMPLETED);
                return result[0].consumed();
            });
        } catch (RuntimeException | Error error) {
            try { execution.finish(ExecutionSession.EndReason.ERROR); }
            catch (RuntimeException | Error cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
        return result[0];
    }
}
