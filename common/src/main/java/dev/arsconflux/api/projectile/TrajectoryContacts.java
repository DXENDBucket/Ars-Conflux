package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Adapter boundary around form collision dispatch. Never advances, teleports or revives a carrier. */
public final class TrajectoryContacts {
    private static final ThreadLocal<TrajectoryContact> CURRENT=new ThreadLocal<>();
    private TrajectoryContacts(){}
    public static TrajectoryContact current(EntityProjectileSpell carrier){
        var frame=CURRENT.get();return frame!=null && frame.carrier()==carrier?frame:null;
    }
    public static boolean isOwnVisual(EntityProjectileSpell carrier,net.minecraft.world.entity.Entity target){
        var motion=ArsTrajectory.managed(carrier);
        return motion!=null && motion.visual()==target || target instanceof CarrierProxy proxy && proxy.confluxTrajectory()==carrier;
    }
    public static void collide(EntityProjectileSpell carrier,HitResult hit,Runnable action){run(carrier,hit,true,action);}
    /** Use for area/timed forms, including expiration pulses. Pulses cannot retain an expired carrier. */
    public static void pulse(EntityProjectileSpell carrier,Runnable action){
        run(carrier,BlockHitResult.miss(carrier.position(),Direction.UP,carrier.blockPosition()),false,action);
    }
    private static void run(EntityProjectileSpell carrier,HitResult hit,boolean physical,Runnable action){
        var motion=ArsTrajectory.managed(carrier);
        if(motion==null || !motion.hasContactPolicy() || current(carrier)!=null){action.run();return;}
        var previous=CURRENT.get();CURRENT.set(new TrajectoryContact(carrier,hit,physical));
        try{action.run();}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    /** Called only at Ars' contact-consumption point; natural lifetime and explicit removal stay native. */
    public static boolean retains(EntityProjectileSpell carrier){
        var frame=current(carrier);var motion=ArsTrajectory.managed(carrier);
        return frame!=null && frame.physical() && motion!=null && motion.retainContact(frame);
    }
}
