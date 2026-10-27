package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.arsconflux.internal.zero.ZeroMotion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.glyph.AnchorEffect",remap=false)
public abstract class OptionalZeroAnchorMotionMixin {
    @WrapOperation(method="onResolveEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;setPos(DDD)V"))
    private void arsConflux$hold(Entity target,double x,double y,double z,Operation<Void> original,
                                @Local(argsOnly=true) SpellContext context) {
        if(!ZeroMotion.hold(target,new Vec3(x,y,z),context))original.call(target,x,y,z);
    }
}
