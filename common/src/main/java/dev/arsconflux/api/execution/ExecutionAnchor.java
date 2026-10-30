package dev.arsconflux.api.execution;

import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.targeting.Allies;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import java.util.Objects;
import java.util.function.Predicate;

/** Per-trigger physical origin. Does not change ownership, payment or a resolver's chain. */
public final class ExecutionAnchor {
    public enum Basis { FEET, EYES }
    public record Pose(Vec3 origin, Vec3 direction) {}
    private final LivingEntity entity;
    private final Basis basis;
    private final Vec3 offset,point,direction;
    private ExecutionAnchor(LivingEntity entity,Basis basis,Vec3 offset,Vec3 point,Vec3 direction) {
        this.entity=entity;this.basis=basis;this.offset=offset;this.point=point;this.direction=direction;
    }
    public static ExecutionAnchor entity(LivingEntity entity,Basis basis,Vec3 offset) {
        Objects.requireNonNull(entity);Objects.requireNonNull(basis);requireFinite(offset);
        return new ExecutionAnchor(entity,basis,offset,null,null);
    }
    public static ExecutionAnchor position(Vec3 point,Vec3 direction) {
        requireFinite(point);requireFinite(direction);
        return new ExecutionAnchor(null,Basis.FEET,Vec3.ZERO,point,unit(direction));
    }
    /** Entity permissions use the caster, never damage credit. Block hits remain valid origins. */
    public static ExecutionAnchor beneficial(CastContext cast,Basis basis,Vec3 offset) {
        if(cast.target() instanceof EntityHitResult hit) {
            if(!(hit.getEntity() instanceof LivingEntity actor) || !mayExecute(cast,actor))return null;
            return entity(actor,basis,offset);
        }
        if(cast.target() instanceof BlockHitResult hit && hit.getType()==HitResult.Type.BLOCK) {
            Vec3 incoming=cast.incomingDirection();
            Vec3 fallback=cast.caster()==null?new Vec3(0,0,1):viewDirection(cast.caster());
            return position(hit.getLocation(),incoming!=null && finite(incoming) && incoming.lengthSqr()>1e-10?incoming:fallback);
        }
        return null;
    }
    public static boolean mayExecute(CastContext cast,LivingEntity actor) {
        return actor!=null && actor.isAlive() && !actor.isRemoved() && !actor.isSpectator()
                && Allies.areAllies(cast.caster(),actor);
    }
    public LivingEntity entity(){return entity;}
    public Basis basis(){return basis;}
    public Vec3 offset(){return offset;}
    public Pose sample(){
        return entity==null?new Pose(point,direction):new Pose((basis==Basis.EYES?entity.getEyePosition():entity.position()).add(offset),viewDirection(entity));
    }
    /** Current view direction; living executors use head yaw even when their body faces elsewhere. */
    public static Vec3 viewDirection(Entity entity){return unit(Objects.requireNonNull(entity).getViewVector(1.0F));}
    public boolean valid(ServerLevel world,CastContext cast){
        if(entity!=null && (entity.level()!=world || !mayExecute(cast,entity)))return false;
        Vec3 origin=sample().origin();
        return finite(origin) && world.hasChunkAt(BlockPos.containing(origin));
    }
    /** Neutral presence check for providers with different, e.g. harmful-control, authorization. */
    public boolean present(ServerLevel world){
        if(entity!=null && (entity.level()!=world || !entity.isAlive() || entity.isRemoved() || entity.isSpectator()))return false;
        Vec3 origin=sample().origin();return finite(origin) && world.hasChunkAt(BlockPos.containing(origin));
    }
    public boolean valid(ServerLevel world,Predicate<LivingEntity> permission){
        Objects.requireNonNull(permission);
        return present(world) && (entity==null || permission.test(entity));
    }
    private static boolean finite(Vec3 v){return v!=null && Double.isFinite(v.lengthSqr());}
    private static void requireFinite(Vec3 v){if(!finite(v))throw new IllegalArgumentException("Anchor coordinates must be finite");}
    private static Vec3 unit(Vec3 v){return finite(v) && v.lengthSqr()>1e-10?v.normalize():new Vec3(0,0,1);}
}
