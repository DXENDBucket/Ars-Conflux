package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.context.TriggerGeometry;
import dev.arsconflux.api.context.ResolverContexts;
import dev.arsconflux.platform.ArsAccess;
import dev.arsconflux.api.execution.ExecutionSession;
import dev.arsconflux.api.execution.ExecutionSessions;
import dev.arsconflux.internal.EntityTransfers;
import dev.arsconflux.mixin.SpellContextAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.function.Consumer;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Reuse an Ars/addon carrier's movement and collisions with one networked replacement entity.
 * The replacement's native tick must not also move or apply its own collisions while managed.
 * An addon retains responsibility for those native hooks, ownership, payload and billing.
 * The supplied Ars carrier remains off-world, and must not be spawned separately. */
public final class ArsTrajectory {
    private static final Map<EntityProjectileSpell, WeakReference<ArsTrajectory>> MANAGED = new WeakHashMap<>();
    private static final Map<Entity, WeakReference<ArsTrajectory>> VISUALS = new WeakHashMap<>();
    private final Entity visual;
    private final EntityProjectileSpell carrier;
    private ExecutionSession<?> session;
    private ServerLevel pendingDestination;
    public ArsTrajectory(Entity visual, EntityProjectileSpell carrier) { this.visual = visual; this.carrier = carrier; }
    public EntityProjectileSpell carrier() { return carrier; }
    public Vec3 incomingDirection() { return TriggerGeometry.direction(carrier); }
    static ArsTrajectory managed(EntityProjectileSpell carrier) {
        var reference=MANAGED.get(carrier); return reference==null?null:reference.get();
    }
    ExecutionSession<?> owningSession() { return session; }
    Entity visual() { return visual; }
    static ArsTrajectory forVisual(Entity entity) {
        var reference=VISUALS.get(entity); return reference==null?null:reference.get();
    }
    /** Adapter boundary for native carrier teleportation. Position remains controlled by the native adapter. */
    public static boolean redirect(EntityProjectileSpell carrier, ServerLevel destination) {
        var reference = MANAGED.get(carrier);
        var trajectory = reference == null ? null : reference.get();
        if (trajectory == null) return false;
        trajectory.pendingDestination = destination;
        EntityTransfers.carrier(carrier, destination);
        return true;
    }
    public boolean start(ServerLevel world, Runnable configure, Consumer<HitResult> impact) {
        session = ExecutionSessions.current();
        if (carrier instanceof TrailBudget trail) trail.confluxBudget(Integer.MAX_VALUE);
        visual.setPos(carrier.position()); visual.setDeltaMovement(carrier.getDeltaMovement());
        configure.run();
        var original = ArsAccess.resolver(carrier);
        ArsAccess.resolver(carrier, ResolverContexts.inherit(original,
                new ImpactResolver(original.spellContext.clone(), original.castType, original, impact)));
        boolean spawned = world.addFreshEntity(visual);
        if (spawned) {
            MANAGED.put(carrier, new WeakReference<>(this));
            VISUALS.put(visual, new WeakReference<>(this));
            if (session != null) session.trackProjectile(visual);
        }
        return spawned;
    }
    public boolean tick(ServerLevel world) {
        if (visual.isRemoved() || carrier.isRemoved()) return true;
        if (pendingDestination != null && !transfer(carrier.position())) return true;
        world = (ServerLevel)carrier.level();
        if (!world.hasChunkAt(carrier.blockPosition()) || !world.hasChunkAt(BlockPos.containing(carrier.getNextHitPosition()))) return true;
        ++carrier.tickCount; carrier.tick();
        if (pendingDestination != null && !carrier.isRemoved() && !transfer(carrier.position())) return true;
        visual.setPos(carrier.position()); visual.setDeltaMovement(carrier.getDeltaMovement());
        // Orbit has no linear velocity; predict its next point for client movement.
        if (carrier.getDeltaMovement().lengthSqr() < 1.0e-8)
            visual.setDeltaMovement(carrier.getNextHitPosition().subtract(carrier.position()).scale(1.0 / 3.0));
        var velocity = visual.getDeltaMovement();
        visual.setYRot((float)(Math.atan2(velocity.x, velocity.z) * 180 / Math.PI));
        visual.setXRot((float)(Math.atan2(velocity.y, velocity.horizontalDistance()) * 180 / Math.PI));
        visual.hasImpulse = true;
        return carrier.isRemoved();
    }
    private boolean transfer(Vec3 position) {
        var destination = pendingDestination;
        if (destination == null) return true;
        pendingDestination = null;
        carrier.setPos(position);
        var displacement = position.subtract(visual.position());
        boolean success = session == null
                ? EntityTransfers.projectile(visual, destination, position)
                : session.relocateProjectiles(destination, displacement);
        if (!success) { close(); return false; }
        visual.setPos(position);
        return true;
    }
    public void close() { MANAGED.remove(carrier); VISUALS.remove(visual); visual.discard(); carrier.discard(); }
    private final class ImpactResolver extends SpellResolver {
        private final Consumer<HitResult> impact;
        private final SpellResolver focusSource;
        private ImpactResolver(SpellContext context, AbstractCastMethod castType, SpellResolver focusSource, Consumer<HitResult> impact) {
            super(context); this.castType = castType; this.focusSource = focusSource; this.impact = impact;
        }
        @Override public boolean hasFocus(ItemStack stack) { return focusSource.hasFocus(stack); }
        @Override public boolean hasFocus(Item item) { return focusSource.hasFocus(item); }
        @Override public void onResolveEffect(Level world, HitResult hit) {
            if (world.isClientSide || !transfer(hit.getLocation())) return;
            ((SpellContextAccess)spellContext).arsConflux$level(world);
            ((SpellContextAccess)focusSource.spellContext).arsConflux$level(world);
            impact.accept(hit);
        }
        @Override public SpellResolver getNewResolver(SpellContext context) {
            return ResolverContexts.inherit(this, new ImpactResolver(context, castType, focusSource, impact));
        }
    }
}
