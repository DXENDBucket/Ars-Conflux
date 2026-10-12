package dev.arsconflux;

import dev.arsconflux.api.casting.MovementLimits;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementLimitsTest {
    @Test void independentRestrictionsComposeOnceInEitherOrder() {
        var frame = new Object();
        float velocity = MovementLimits.multiplier(frame, .2f);
        velocity *= MovementLimits.multiplier(frame, .2f);
        assertEquals(.2f, velocity, .0001f);
        velocity *= MovementLimits.multiplier(frame, .1f);
        assertEquals(.1f, velocity, .0001f);
        assertEquals(.2f, MovementLimits.multiplier(new Object(), .2f), .0001f);
    }
    @Test void fullStopAndNativeSpeedModifiersRemainFinite() {
        var frame = new Object();
        float velocity = MovementLimits.multiplier(frame, 1.2f);
        velocity *= MovementLimits.multiplier(frame, .2f);
        assertEquals(.2f, velocity, .0001f);
        velocity *= MovementLimits.multiplier(frame, 0);
        velocity *= MovementLimits.multiplier(frame, .2f);
        assertEquals(0, velocity);
        assertEquals(1, MovementLimits.multiplier(frame, Float.NaN));
    }
}
