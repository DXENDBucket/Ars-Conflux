package dev.arsconflux.api.projectile;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.Objects;
import java.util.function.Supplier;

/** A synchronous native-impact receipt, separate from damage scaling, payment and projectile lifetime.
 * Continuations run after the observation closes; their damage cannot authorize the native hit. */
public final class ImpactTriggers {
    public enum Policy { ENTITY_DAMAGE, ACTIVATION }
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();
    private static final class Frame {
        final Entity expected;
        int depth;
        boolean damaged;
        Frame(Entity expected) { this.expected = expected; }
    }
    private ImpactTriggers() {}
    /** Always execute the complete native callback. Block hits and explicit non-damage activations
     * may continue without hurt; entity damage requires an accepted hit on that exact entity. */
    public static boolean resolve(HitResult hit, Policy policy, Runnable nativeImpact) {
        Objects.requireNonNull(hit); Objects.requireNonNull(policy); Objects.requireNonNull(nativeImpact);
        var previous = CURRENT.get();
        var frame = new Frame(hit instanceof EntityHitResult entity ? entity.getEntity() : null);
        CURRENT.set(frame);
        try { nativeImpact.run(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
        return hit.getType() != HitResult.Type.MISS && (hit.getType() == HitResult.Type.BLOCK
                || policy == Policy.ACTIVATION || frame.damaged);
    }
    /** Call-site adapter for Entity overrides or third-party damage functions. The native boolean
     * is returned unchanged. Nested hurt listeners cannot credit an otherwise rejected outer hit. */
    public static boolean damage(Entity target, Supplier<Boolean> action) {
        var frame = CURRENT.get();
        if (frame == null) return action.get();
        boolean root = frame.depth++ == 0;
        try {
            boolean result = action.get();
            if (root && result && target == frame.expected) frame.damaged = true;
            return result;
        } finally { frame.depth--; }
    }
}
