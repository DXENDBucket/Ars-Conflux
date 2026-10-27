package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.internal.projectile.MotionControlData;
import net.minecraft.network.syncher.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityProjectileSpell.class)
public abstract class ProjectileControlDataMixin implements MotionControlData {
    @Unique private static final EntityDataAccessor<Byte> conflux$control=SynchedEntityData.defineId(EntityProjectileSpell.class,EntityDataSerializers.BYTE);
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void arsConflux$define(CallbackInfo ci) { ((EntityProjectileSpell)(Object)this).getEntityData().define(conflux$control,(byte)0); }
    public int conflux$motionFlags() { return ((EntityProjectileSpell)(Object)this).getEntityData().get(conflux$control); }
    public void conflux$motionFlags(int flags) { ((EntityProjectileSpell)(Object)this).getEntityData().set(conflux$control,(byte)flags); }
}
