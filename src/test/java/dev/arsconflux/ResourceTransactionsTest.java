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
    @Test void percentageQuoteIsFrozenAndBothBudgetAndDebitUseIt() {
        var balance=new AtomicInteger(140);var rate=new AtomicInteger(30);var quotes=new AtomicInteger();
        ResourceAccount pool=new ResourceAccount(){
            public Object reservationKey(){return balance;}
            public int quoteCost(int raw){quotes.incrementAndGet();return (raw*(100-rate.get())+99)/100;}
            public boolean canSpend(int amount){return balance.get()>=amount;}
            public void spend(int amount){balance.addAndGet(-amount);}
        };
        var charge=new ResourceCharge(pool,100);
        assertEquals(70,charge.amount());assertEquals(100,charge.originalAmount());assertEquals(2,charge.affordableCount(5));
        rate.set(0);assertTrue(charge.trigger(()->charge.trigger(()->true)));assertEquals(0,balance.get());
        assertEquals(1,quotes.get());assertEquals(70,ResourceCharge.exact(pool,70).amount());assertEquals(1,quotes.get());
    }
    @Test void decimalPercentageRoundingAndFullReductionAreSafe() {
        assertEquals(7,ArsManaDiscounts.apply(10,new java.math.BigDecimal("0.3")));
        assertEquals(1,ArsManaDiscounts.apply(1,new java.math.BigDecimal("0.9")));
        assertEquals(0,ArsManaDiscounts.apply(123,java.math.BigDecimal.ONE));
        assertEquals(0,ArsManaDiscounts.apply(0,new java.math.BigDecimal("0.2")));
        assertEquals(Integer.MAX_VALUE,ArsManaDiscounts.apply(Integer.MAX_VALUE,java.math.BigDecimal.ZERO));
    }
}
