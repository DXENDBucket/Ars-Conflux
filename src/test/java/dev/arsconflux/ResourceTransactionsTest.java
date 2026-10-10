package dev.arsconflux;

import dev.arsconflux.api.resource.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

final class ResourceTransactionsTest {
    private record Pool(Object reservationKey, AtomicInteger balance) implements ResourceAccount {
        public boolean canSpend(int amount) { return balance.get() >= amount; }
        public void spend(int amount) { balance.addAndGet(-amount); }
    }
    @Test void separateAddonWrappersCannotSpendTheirParentsReservation() {
        var balance = new AtomicInteger(100);
        var iron = new ResourceCharge(new Pool(ResourcePoolKey.of("test:mana", balance), balance), 70);
        var goety = new ResourceCharge(new Pool(ResourcePoolKey.of("test:mana", balance), balance), 40);
        assertTrue(iron.trigger(() -> {
            assertFalse(goety.trigger(() -> fail("Must not execute an unpaid nested effect")));
            return true;
        }));
        assertEquals(30, balance.get());
    }
    @Test void differentResourcesOnOneActorRemainIndependent() {
        var mana = new AtomicInteger(100); var souls = new AtomicInteger(100);
        var actor = new Object();
        assertTrue(new ResourceCharge(new Pool(ResourcePoolKey.of("test:mana", actor), mana), 80).trigger(() ->
                new ResourceCharge(new Pool(ResourcePoolKey.of("test:souls", actor), souls), 90).trigger(() -> true)));
        assertEquals(20, mana.get()); assertEquals(10, souls.get());
    }
    @Test void failuresAndExceptionsReleaseReservationWithoutCharging() {
        var balance = new AtomicInteger(100); var charge = new ResourceCharge(new Pool("pool", balance), 100);
        assertFalse(charge.trigger(() -> false));
        assertThrows(IllegalArgumentException.class, () -> charge.trigger(() -> { throw new IllegalArgumentException(); }));
        assertEquals(100, balance.get()); assertTrue(charge.trigger(() -> true)); assertEquals(0, balance.get());
    }
    @Test void committedChildRemainsPaidWhenParentFailsAndHugeVolleysDoNotOverflow() {
        var balance = new AtomicInteger(100); var pool = new Pool("pool", balance);
        assertFalse(new ResourceCharge(pool, 50).trigger(() -> {
            assertTrue(new ResourceCharge(pool, 30).trigger(() -> true)); return false;
        }));
        assertEquals(70, balance.get());
        assertEquals(2, new ResourceCharge(pool, 30).affordableCount(Integer.MAX_VALUE));
        assertEquals(0, new ResourceCharge(pool, Integer.MAX_VALUE).affordableCount(Integer.MAX_VALUE));
    }
}
