package dev.arsconflux.internal.billing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CostSlicesTest {
    @Test void splitDiscountAndCostConserveEveryIntegerIncludingRounding() {
        for (int total = 0; total < 100; total++) {
            int sum = 0; long before = 0;
            for (int weight : new int[]{7, 13, 21, 3}) {
                sum += CostSlices.share(total, before, weight, 44); before += weight;
            }
            assertEquals(total, sum);
        }
    }
    @Test void zeroWeightIsFreeAndLargeQuotesDoNotOverflow() {
        assertEquals(0, CostSlices.share(100, 0, 0, 0));
        assertEquals(Integer.MAX_VALUE, CostSlices.share(Integer.MAX_VALUE, 0, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
