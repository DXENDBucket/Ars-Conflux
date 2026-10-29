package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.internal.projectile.ReflectionStamp;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=EntityProjectileSpell.class,remap=false)
public abstract class ReflectedCarrierHitMixin implements ReflectionStamp {
    @Unique private long conflux$version;
    @Unique private long conflux$collisionStart=-1;
    public long conflux$reflectionVersion() { return conflux$version; }
    public void conflux$markReflection() { conflux$version++; }
    @WrapMethod(method="onHit",remap=true)
    private void conflux$collision(HitResult hit,Operation<Void> original) {
        long previous=conflux$collisionStart; conflux$collisionStart=conflux$version;
        try { dev.arsconflux.api.projectile.TrajectoryContacts.collide((EntityProjectileSpell)(Object)this,hit,()->original.call(hit)); }
        finally { conflux$collisionStart=previous; }
    }
    @Inject(method="attemptRemoval",at=@At("HEAD"),cancellable=true)
    private void conflux$keepReflected(CallbackInfo ci) {
        if(conflux$collisionStart>=0 && conflux$version!=conflux$collisionStart
                || dev.arsconflux.api.projectile.TrajectoryContacts.retains((EntityProjectileSpell)(Object)this))ci.cancel();
    }
}
