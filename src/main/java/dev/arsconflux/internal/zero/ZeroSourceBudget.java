package dev.arsconflux.internal.zero;

/** One initial-source allowance shared by all branches of a single cast. */
public final class ZeroSourceBudget implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final int rawSpellCost;
    private final int paidSource;
    private final boolean sourcePowered;
    private int remaining;

    private ZeroSourceBudget(int rawConvergenceCost, int rawSpellCost, int paidSource,
            boolean sourcePowered) {
        this.rawSpellCost = Math.max(0, rawSpellCost);
        this.paidSource = Math.max(0, paidSource);
        this.sourcePowered = sourcePowered;
        int raw = Math.max(0, rawConvergenceCost);
        this.remaining = sourcePowered ? scaled(Math.min(raw, this.rawSpellCost)) : raw;
    }

    public static ZeroSourceBudget manaPowered(int rawConvergenceCost) {
        return new ZeroSourceBudget(rawConvergenceCost, 0, 0, false);
    }

    public static ZeroSourceBudget sourcePowered(int rawConvergenceCost, int rawSpellCost,
            int paidSource) {
        return new ZeroSourceBudget(rawConvergenceCost, rawSpellCost, paidSource, true);
    }

    private int scaled(int rawCost) {
        if (rawSpellCost == 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, rawCost) * paidSource / rawSpellCost);
    }

    public int claim(int occurrenceCost) {
        int raw = Math.max(0, occurrenceCost);
        int requested = sourcePowered ? scaled(raw) : raw;
        int granted = Math.min(remaining, requested);
        remaining -= granted;
        return granted;
    }
}
