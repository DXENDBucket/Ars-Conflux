package dev.arsconflux.api.casting;

import net.minecraft.world.entity.LivingEntity;

/** Final timing adjustment, after a provider's own equipment and native rules.
 * Values are fractions: 0.25 removes one quarter of the remaining time.
 * Apply once when starting a preparation or recording a new cooldown, never to
 * an existing remaining deadline, channel lifetime, pulse interval or Ars Delay. */
public final class CastingTiming {
    private CastingTiming() {}
    public static int chantTicks(LivingEntity caster, int nativeTicks) {
        return (int)scale(nativeTicks, dev.arsconflux.platform.TimingAttributes.castTimeReduction(caster), Integer.MAX_VALUE);
    }
    public static int cooldownTicks(LivingEntity caster, int nativeTicks) {
        return (int)scale(nativeTicks, dev.arsconflux.platform.TimingAttributes.cooldownReduction(caster), Integer.MAX_VALUE);
    }
    public static long cooldownTicks(LivingEntity caster, long nativeTicks) {
        return scale(nativeTicks, dev.arsconflux.platform.TimingAttributes.cooldownReduction(caster), Long.MAX_VALUE);
    }
    /** Public arithmetic helper for previews and integrations that already have an attribute value. */
    public static long scale(long ticks, double reduction, long maximum) {
        if (ticks <= 0 || maximum <= 0) return 0;
        if (!Double.isFinite(reduction)) reduction = 0;
        reduction = Math.max(-10, Math.min(1, reduction));
        if (reduction == 0) return Math.min(ticks, maximum); // Preserve large raw deadlines exactly.
        if (reduction == 1) return 0;
        double adjusted = Math.ceil(ticks * (1 - reduction));
        return adjusted >= maximum ? maximum : Math.max(1, (long)adjusted);
    }
}
