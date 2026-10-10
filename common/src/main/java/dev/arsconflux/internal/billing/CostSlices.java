package dev.arsconflux.internal.billing;

/** Integer apportionment: a complete pass pays exactly the original total, including rounding. */
public final class CostSlices {
    private CostSlices() {}
    public static int share(int total, long before, long weight, long sum) {
        if (sum <= 0 || weight <= 0 || total <= 0) return 0;
        if (before + weight <= Long.MAX_VALUE / total)
            return (int) (((before + weight) * total / sum) - (before * total / sum));
        var scale = java.math.BigInteger.valueOf(total);
        var divisor = java.math.BigInteger.valueOf(sum);
        return java.math.BigInteger.valueOf(before + weight).multiply(scale).divide(divisor)
                .subtract(java.math.BigInteger.valueOf(before).multiply(scale).divide(divisor)).intValueExact();
    }
}
