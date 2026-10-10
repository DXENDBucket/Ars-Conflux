package dev.arsconflux.api.interaction;

import dev.arsconflux.api.execution.ExecutionDriver;
import dev.arsconflux.api.execution.ExecutionSession;

/** Input-driven execution. Providers own stage data, targeting, payment and completion semantics. */
public interface InteractiveDriver<P> extends ExecutionDriver<P> {
    InputResult input(ExecutionSession<P> session, CastInput input);
}
