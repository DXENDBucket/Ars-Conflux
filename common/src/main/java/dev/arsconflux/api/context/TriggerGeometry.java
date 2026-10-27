package dev.arsconflux.api.context;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import net.minecraft.world.phys.Vec3;

/** Immutable direction snapshot travels with cloned/delayed Ars contexts, independently of their owner. */
public final class TriggerGeometry {
    private static final String KEY = "ars_conflux_incoming";
    private static final dev.arsconflux.internal.ScopedContext<Vec3> CURRENT = new dev.arsconflux.internal.ScopedContext<>();
    private TriggerGeometry() {}
    public static Vec3 canonical(Vec3 direction) {
        return direction!=null && Double.isFinite(direction.lengthSqr()) && direction.lengthSqr()>1.0e-10
                ?direction.normalize():Vec3.ZERO;
    }
    public static void write(SpellContext context, Vec3 direction) {
        if (context == null || direction == null) return;
        if (!Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < 1.0e-10) {clear(context);return;}
        var tag = new net.minecraft.nbt.CompoundTag(); direction = direction.normalize();
        tag.putDouble("x", direction.x); tag.putDouble("y", direction.y); tag.putDouble("z", direction.z);
        context.tag.put(KEY, tag);
    }
    /** Explicit reset, including the legacy Iron snapshot key. Null writes still mean inherit. */
    public static void clear(SpellContext context) {
        if(context!=null){context.tag.remove(KEY);context.tag.remove("irons_nouveau_incoming");}
    }
    public static Vec3 read(SpellContext context) {
        if (CURRENT.current() != null) return CURRENT.current();
        if (context == null) return Vec3.ZERO;
        String key = context.tag.contains(KEY) ? KEY : "irons_nouveau_incoming";
        if (!context.tag.contains(key)) return Vec3.ZERO;
        var tag = context.tag.getCompound(key);
        return new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")).normalize();
    }
    public static void capture(SpellContext context) { if (CURRENT.current() != null) write(context, CURRENT.current()); }
    public static Vec3 direction(EntityProjectileSpell projectile) {
        var direction = projectile.getDeltaMovement();
        return direction.lengthSqr() > 1.0e-10 ? direction.normalize()
                : projectile.getNextHitPosition().subtract(projectile.position()).normalize();
    }
    public static void scoped(Vec3 direction, Runnable action) {
        CURRENT.run(direction==null?null:canonical(direction),action);
    }
}
