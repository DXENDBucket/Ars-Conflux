package dev.arsconflux.api.casting;

import net.minecraft.network.chat.Component;

/** Read-only reason supplied by a provider. A negative duration means unknown, not zero. */
public record CastRejection(Component message, long remainingTicks) {
    public static CastRejection cooldown(Component name, long ticks) {
        return new CastRejection(ticks < 0
                ? Component.translatable("ars_conflux.cast.cooldown_unknown", name)
                : Component.translatable("ars_conflux.cast.cooldown", name,
                    String.format(java.util.Locale.ROOT, "%.1f", Math.max(0, ticks) / 20.0)), ticks);
    }
}
