package dev.arsconflux.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsconflux.api.projectile.CarrierProxy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;

/** String-only optional target: exclude the visible proxy before Trail consumes its target budget. */
@Pseudo
@Mixin(targets = "alexthw.not_enough_glyphs.common.spell.TrailingProjectile", remap = false)
public abstract class OptionalTrailTargetsMixin implements dev.arsconflux.api.projectile.TrailBudget {
    @Shadow public int maxProcs;
    @Override public void confluxBudget(int value) { maxProcs = value; }
    @WrapOperation(method = "castSpells", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;", remap = true))
    private List<Entity> arsConflux$excludeOwnVisual(Level world, Entity except, AABB box, Operation<List<Entity>> original) {
        return original.call(world, except, box).stream().filter(entity -> {
            if (entity instanceof CarrierProxy proxy && proxy.confluxTrajectory() == (Object)this) {
                maxProcs = Integer.MAX_VALUE; return false;
            }
            return true;
        }).toList();
    }
}
