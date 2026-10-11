package dev.arsconflux.api.context;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.Supplier;

/** The attacker of a synchronous damage callback, before vanilla updates damage history. */
public final class RetaliationTargets {
    private record Frame(LivingEntity victim, DamageSource source, Frame parent) {}
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();
    private RetaliationTargets() {}
    public static LivingEntity currentAttacker(LivingEntity victim) {
        for (var frame = CURRENT.get(); frame != null; frame = frame.parent()) {
            if (frame.victim() == victim)
                return frame.source().getEntity() instanceof LivingEntity attacker && attacker != victim ? attacker : null;
        }
        return null;
    }
    public static boolean isTakingDamage(LivingEntity victim) {
        for (var frame = CURRENT.get(); frame != null; frame = frame.parent())
            if (frame.victim() == victim) return true;
        return false;
    }
    public static <T> T during(LivingEntity victim, DamageSource source, Supplier<T> action) {
        var previous = CURRENT.get();
        CURRENT.set(new Frame(victim, source, previous));
        try { return action.get(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
}
