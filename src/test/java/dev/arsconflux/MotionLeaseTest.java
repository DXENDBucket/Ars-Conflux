package dev.arsconflux;

import dev.arsconflux.internal.projectile.MotionLease;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MotionLeaseTest {
    @Test void interruptedHoldsExpireAndContinuousHoldsRenew() {
        var state=new MotionLease(false); var owner=new Object();
        state.hold(owner,12); assertTrue(state.held(11));
        state.hold(owner,13); assertTrue(state.held(12));
        assertFalse(state.held(13)); assertFalse(state.noGravity(13));
    }
    @Test void obsoleteOwnerCannotReleaseANewerHold() {
        var state=new MotionLease(false); var old=new Object(); var current=new Object();
        state.hold(old,12); state.hold(current,13); state.release(old);
        assertTrue(state.held(11)); state.release(current); assertFalse(state.held(11));
    }
    @Test void gravitySuppressionAndHoldingRestoreInEitherOrder() {
        var owner=new Object();
        var first=new MotionLease(false); first.suppress(true); first.hold(owner,12);
        first.release(owner); assertTrue(first.noGravity(10));
        first.suppress(false); assertFalse(first.noGravity(10));
        var second=new MotionLease(false); second.hold(owner,12); second.suppress(true);
        second.suppress(false); assertTrue(second.noGravity(10));
        second.release(owner); assertFalse(second.noGravity(10));
    }
    @Test void launchingReleasesAHoldWithoutRemovingGravitySuppression() {
        var state=new MotionLease(false); state.hold(new Object(),12); state.suppress(true);
        state.launch(); assertFalse(state.held(10)); assertTrue(state.suppressed());
        assertTrue(state.noGravity(10)); state.suppress(false); assertFalse(state.active(10));
    }
    @Test void naturallyWeightlessCarriersRemainWeightlessAfterControlsEnd() {
        var state=new MotionLease(true); state.hold(new Object(),12); state.suppress(true);
        state.launch(); state.suppress(false); assertFalse(state.active(10)); assertTrue(state.noGravity(10));
    }
}
