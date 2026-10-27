package dev.arsconflux;

import dev.arsconflux.api.casting.ActiveUseCommit;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ActiveUseCommitTest {
    @Test void phasesAcknowledgeOnlyOnceAndFailedBeginDoesNotCommit() {
        var use = new ActiveUseCommit(); var calls = new AtomicInteger();
        use.acknowledge(false, calls::incrementAndGet);
        assertEquals(0, calls.get());
        use.acknowledge(true, calls::incrementAndGet);
        for (int i = 0; i < 1000; i++) use.acknowledge(true, calls::incrementAndGet);
        assertEquals(1, calls.get());
    }
    @Test void cancellationAndAnInterruptedCommitCannotBeReplayed() {
        var cancelled = new ActiveUseCommit(); cancelled.close();
        cancelled.acknowledge(true, () -> fail("Cancelled use committed"));
        assertFalse(cancelled.open());
        var interrupted = new ActiveUseCommit();
        assertThrows(IllegalStateException.class, () -> interrupted.acknowledge(true, () -> { throw new IllegalStateException(); }));
        interrupted.acknowledge(true, () -> fail("Partially applied activation was replayed"));
    }
}
