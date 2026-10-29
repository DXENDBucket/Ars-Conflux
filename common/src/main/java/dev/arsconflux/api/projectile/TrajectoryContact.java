package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.execution.TriggerPayment;
import net.minecraft.world.phys.HitResult;
import java.util.function.BooleanSupplier;

/** One physical contact, or one form-owned pulse that can resolve several targets.
 * Target expansion never creates another contact. Payment is shared only by this frame;
 * effects and their own continuation costs still execute independently for each target. */
public final class TrajectoryContact {
    private final EntityProjectileSpell carrier;
    private final HitResult hit;
    private final boolean physical;
    private boolean paid;
    TrajectoryContact(EntityProjectileSpell carrier,HitResult hit,boolean physical){
        this.carrier=carrier;this.hit=hit;this.physical=physical;
    }
    public EntityProjectileSpell carrier(){return carrier;}
    public HitResult hit(){return hit;}
    public boolean physical(){return physical;}
    /** A rejected action refunds normally; a later target may still commit this contact's payment. */
    public boolean activate(TriggerPayment payment,BooleanSupplier action){
        if(TrajectoryContacts.current(carrier)!=this)throw new IllegalStateException("Contact frame is no longer active");
        if(paid)return action.getAsBoolean();
        boolean success=payment.trigger(action);
        if(success)paid=true;
        return success;
    }
}
