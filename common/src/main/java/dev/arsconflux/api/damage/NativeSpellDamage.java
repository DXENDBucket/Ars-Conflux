package dev.arsconflux.api.damage;

import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.platform.DamageAccess;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Explicit opt-in to Ars equipment damage and Pre/Post events for an adapter's native hits.
 * Native base damage, amplification, source types and secondary effects remain provider-owned.
 * This API neither spends resources nor replaces the native source with an Ars source. */
public final class NativeSpellDamage {
    private static final Map<Entity, CastContext> CONTEXTS = new WeakHashMap<>();
    private static final ThreadLocal<CastContext> SCOPE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> DISPATCHING = ThreadLocal.withInitial(() -> false);

    @FunctionalInterface
    public interface Hit {
        boolean hurt(DamageSource source, float amount);
    }

    /** Attach a transient context; null explicitly removes an earlier attachment. Server-thread only. */
    public static <T extends Entity> T bind(T entity, CastContext context) {
        Objects.requireNonNull(entity, "entity");
        if (context == null) CONTEXTS.remove(entity);
        else CONTEXTS.put(entity, context);
        return entity;
    }

    public static CastContext context(Entity entity) {
        return CONTEXTS.get(entity);
    }

    /** Context for synchronous native LivingEntity.hurt calls inside during(), e.g. an explosion. */
    public static CastContext currentScope() {
        return SCOPE.get();
    }

    /** Opt in only the synchronous action. Delayed children must be bound or explicitly passed context.
     * Nested calls restore the previous scope, including on exceptions. Null masks an outer scope. */
    public static <T> T during(CastContext context, Supplier<T> action) {
        var previous = SCOPE.get();
        if (context == null) SCOPE.remove();
        else SCOPE.set(context);
        try {
            return action.get();
        } finally {
            if (previous == null) SCOPE.remove();
            else SCOPE.set(previous);
        }
    }

    /** Apply the caster's Ars flat bonus, dispatch Pre, call the native hit, then Post on success.
     * A shared guard avoids duplicate scaling when provider hooks and the scoped hurt hook overlap.
     * Call only for amounts not already processed by Ars or another bridge. */
    public static boolean hurt(CastContext context, Entity target, DamageSource source, float amount, Hit original) {
        return dev.arsconflux.api.projectile.ImpactTriggers.damage(target, () -> apply(context, target, source, amount, original));
    }
    private static boolean apply(CastContext context, Entity target, DamageSource source, float amount, Hit original) {
        if (context == null || target.level().isClientSide || DISPATCHING.get())
            return original.hurt(source, amount);
        var caster = context.caster();
        if (caster == null || !caster.isAlive() || !Float.isFinite(amount) || amount <= 0)
            return original.hurt(source, amount);
        float adjusted = (float)Math.max(0, amount + DamageAccess.bonus(caster));
        DISPATCHING.set(true);
        try {
            var pre = DamageAccess.pre(source, caster, target, adjusted, context.ars());
            if (pre.isCanceled() || !Float.isFinite(pre.damage) || pre.damage <= 0) return false;
            boolean hit = original.hurt(pre.damageSource, pre.damage);
            if (hit) DamageAccess.post(pre.damageSource, caster, target, pre.damage, context.ars());
            return hit;
        } finally {
            DISPATCHING.remove();
        }
    }

    /** Lifecycle cleanup for transient entity attachments. */
    public static void unload(Level world) {
        CONTEXTS.keySet().removeIf(entity -> entity.level() == world);
    }

    public static void stop() {
        CONTEXTS.clear();
        SCOPE.remove();
        DISPATCHING.remove();
    }

    private NativeSpellDamage() {}
}
