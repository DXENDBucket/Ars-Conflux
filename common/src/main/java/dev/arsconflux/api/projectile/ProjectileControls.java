package dev.arsconflux.api.projectile;

import dev.arsconflux.internal.projectile.MotionLease;
import dev.arsconflux.internal.projectile.MotionControlData;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Server-thread motion controls for Ars projectiles and paired native visuals/off-world carriers.
 * This does not transfer ownership, payment or the native carrier's targeting/path rules. */
public final class ProjectileControls {
    private static final Map<EntityProjectileSpell,Control> CONTROLS=new WeakHashMap<>();
    private record Flight(EntityProjectileSpell carrier,ArsTrajectory trajectory) {
        List<Entity> members() { return trajectory==null?List.of(carrier):trajectory.members(); }
    }
    private static final class Control {
        final MotionLease lease;
        WeakReference<Entity> anchor=new WeakReference<>(null);
        Control(EntityProjectileSpell carrier) { lease=new MotionLease(carrier.isNoGravity()); }
    }
    private ProjectileControls() {}
    private static Flight motion(Entity entity) {
        var motion=ArsTrajectory.forEntity(entity);
        if(entity.level().isClientSide || entity.isRemoved())return null;
        if(motion!=null)return !motion.visual().isRemoved() && !motion.carrier().isRemoved()
                && entity.level()==motion.carrier().level()?new Flight(motion.carrier(),motion):null;
        return entity instanceof EntityProjectileSpell carrier?new Flight(carrier,null):null;
    }
    private static boolean finite(Vec3 value) {
        return value!=null && Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }
    /** Set a launch velocity for the entire flight; release any hold, retaining gravity suppression. */
    public static boolean velocity(Entity entity,Vec3 velocity) {
        var motion=motion(entity); if(motion==null || !finite(velocity))return false;
        var control=CONTROLS.get(motion.carrier());
        if(control!=null) { control.lease.launch(); applyGravity(motion,control); }
        if(control!=null && control.lease.suppressed())velocity=new Vec3(velocity.x,0,velocity.z);
        motion.carrier().setDeltaMovement(velocity); motion.carrier().hasImpulse=true;
        for(var member:motion.members()) { member.setDeltaMovement(velocity); member.hasImpulse=true; }
        return true;
    }
    /** Renew a two-tick hold. The most recent holder owns it; obsolete holders cannot release it.
     * Expiration is a fallback for an interrupted producer; normal cleanup should call release(). */
    public static boolean hold(Entity entity,Object owner,Vec3 position) {
        var motion=motion(entity); if(motion==null || owner==null || !finite(position))return false;
        var control=CONTROLS.computeIfAbsent(motion.carrier(),Control::new);
        control.lease.hold(owner,entity.level().getGameTime()+2);
        control.anchor=new WeakReference<>(entity);
        var delta=position.subtract(entity.position());
        if(motion.trajectory()!=null)motion.carrier().setPos(motion.carrier().position().add(delta));
        motion.carrier().setDeltaMovement(Vec3.ZERO);
        for(var member:motion.members()) {
            member.setPos(member.position().add(delta)); member.setDeltaMovement(Vec3.ZERO); member.hasImpulse=true;
        }
        applyGravity(motion,control); return true;
    }
    public static void release(Entity entity,Object owner) {
        var motion=motion(entity); var control=motion==null?null:CONTROLS.get(motion.carrier());
        if(control!=null) { control.lease.release(owner); applyGravity(motion,control); }
    }
    public static void suppressGravity(Entity entity) {
        var motion=motion(entity); if(motion==null)return;
        var control=CONTROLS.computeIfAbsent(motion.carrier(),Control::new);
        control.lease.suppress(true); applyGravity(motion,control); flatten(motion);
    }
    public static void restoreGravity(Entity entity) {
        var motion=motion(entity); var control=motion==null?null:CONTROLS.get(motion.carrier());
        if(control!=null) { control.lease.suppress(false); applyGravity(motion,control); }
    }
    private static void applyGravity(Flight motion,Control control) {
        long now=motion.carrier().level().getGameTime();
        // EntityProjectileSpell overrides isNoGravity() with its own public field.
        boolean value=control.lease.noGravity(now);
        motion.carrier().isNoGravity=value; motion.carrier().setNoGravity(value);
        if(motion.carrier() instanceof MotionControlData data)
            data.conflux$motionFlags((control.lease.held(now)?1:0)|(control.lease.suppressed()?2:0));
        if(!control.lease.active(now))CONTROLS.remove(motion.carrier());
    }
    private static void flatten(Flight motion) {
        var velocity=motion.carrier().getDeltaMovement();
        motion.carrier().setDeltaMovement(velocity.x,0,velocity.z);
        for(var member:motion.members()) {
            var nativeVelocity=member.getDeltaMovement(); member.setDeltaMovement(nativeVelocity.x,0,nativeVelocity.z); member.hasImpulse=true;
        }
    }
    static boolean beforeTick(ArsTrajectory motion) {
        return CONTROLS.containsKey(motion.carrier()) && beforeTick(new Flight(motion.carrier(),motion));
    }
    private static boolean beforeTick(Flight motion) {
        var control=CONTROLS.get(motion.carrier()); if(control==null)return false;
        applyGravity(motion,control);
        if(control.lease.suppressed())flatten(motion);
        if(!control.lease.held(motion.carrier().level().getGameTime()))return false;
        var target=control.anchor.get();
        if(target!=null && !target.isRemoved()) {
            motion.carrier().setYRot(target.getViewYRot(1)); motion.carrier().setXRot(target.getViewXRot(1));
            for(var member:motion.members()) {
                member.setYRot(target.getViewYRot(1)); member.setXRot(target.getViewXRot(1)); member.hasImpulse=true;
            }
        }
        return true;
    }
    static void afterTick(ArsTrajectory motion) {
        if(CONTROLS.containsKey(motion.carrier()))afterTick(new Flight(motion.carrier(),motion));
    }
    private static void afterTick(Flight motion) {
        var control=CONTROLS.get(motion.carrier());
        if(control!=null && control.lease.suppressed())flatten(motion);
    }
    /** Native Ars tick adapter. Off-world carriers are already handled by ArsTrajectory. */
    public static boolean beforeNativeTick(EntityProjectileSpell carrier) {
        if(carrier.level().isClientSide) {
            int flags=((MotionControlData)carrier).conflux$motionFlags();
            if((flags&2)!=0)flattenClient(carrier);
            return (flags&1)!=0;
        }
        return !carrier.level().isClientSide && CONTROLS.containsKey(carrier)
                && ArsTrajectory.managed(carrier)==null && beforeTick(new Flight(carrier,null));
    }
    public static void afterNativeTick(EntityProjectileSpell carrier) {
        if(carrier.level().isClientSide) {
            if((((MotionControlData)carrier).conflux$motionFlags()&2)!=0)flattenClient(carrier);
            return;
        }
        if(!carrier.level().isClientSide && CONTROLS.containsKey(carrier)
                && ArsTrajectory.managed(carrier)==null)afterTick(new Flight(carrier,null));
    }
    private static void flattenClient(EntityProjectileSpell carrier) {
        var velocity=carrier.getDeltaMovement(); carrier.setDeltaMovement(velocity.x,0,velocity.z);
    }
    static void close(ArsTrajectory motion) { CONTROLS.remove(motion.carrier()); }
}
