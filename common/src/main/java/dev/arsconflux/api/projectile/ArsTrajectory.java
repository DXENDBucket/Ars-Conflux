package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.context.TriggerGeometry;
import dev.arsconflux.api.context.ResolverContexts;
import dev.arsconflux.platform.ArsAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.function.Consumer;

/** Reuse an Ars/addon carrier's movement and collisions with one networked replacement entity.
 * The replacement's native tick must not also move or apply its own collisions while managed.
 * An addon retains responsibility for those native hooks, ownership, payload and billing.
 * The supplied Ars carrier remains off-world, and must not be spawned separately. */
public final class ArsTrajectory {
    private final Entity visual;
    private final EntityProjectileSpell carrier;
    public ArsTrajectory(Entity visual, EntityProjectileSpell carrier) { this.visual = visual; this.carrier = carrier; }
    public EntityProjectileSpell carrier() { return carrier; }
    public Vec3 incomingDirection() { return TriggerGeometry.direction(carrier); }
    public boolean start(ServerLevel world, Runnable configure, Consumer<HitResult> impact) {
        if (carrier instanceof TrailBudget trail) trail.confluxBudget(Integer.MAX_VALUE);
        visual.setPos(carrier.position()); visual.setDeltaMovement(carrier.getDeltaMovement());
        configure.run();
        var original = ArsAccess.resolver(carrier);
        ArsAccess.resolver(carrier, ResolverContexts.inherit(original,
                new ImpactResolver(original.spellContext.clone(), original.castType, impact)));
        return world.addFreshEntity(visual);
    }
    public boolean tick(ServerLevel world) {
        if (visual.isRemoved() || carrier.isRemoved()) return true;
        if (!world.hasChunkAt(carrier.blockPosition()) || !world.hasChunkAt(BlockPos.containing(carrier.getNextHitPosition()))) return true;
        ++carrier.tickCount; carrier.tick();
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
    public void close() { visual.discard(); carrier.discard(); }
    private static final class ImpactResolver extends SpellResolver {
        private final Consumer<HitResult> impact;
        private ImpactResolver(SpellContext context, AbstractCastMethod castType, Consumer<HitResult> impact) {
            super(context); this.castType = castType; this.impact = impact;
        }
        @Override public void onResolveEffect(Level world, HitResult hit) { if (!world.isClientSide) impact.accept(hit); }
        @Override public SpellResolver getNewResolver(SpellContext context) {
            return ResolverContexts.inherit(this, new ImpactResolver(context, castType, impact));
        }
    }
}
