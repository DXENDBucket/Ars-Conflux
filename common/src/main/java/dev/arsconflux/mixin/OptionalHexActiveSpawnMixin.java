package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsconflux.api.casting.ActiveCasts;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.ProjectileDispatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "io.yukkuric.hex_ars_link.action.OpShootCast$Action", remap = false)
public abstract class OptionalHexActiveSpawnMixin {
    @WrapOperation(method = "cast(Lat/petrak/hexcasting/api/casting/eval/CastingEnvironment;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private boolean arsConflux$spawn(ServerLevel world, Entity entity, Operation<Boolean> original) {
        boolean success = ProjectileDispatch.convert(entity) || original.call(world, entity);
        if (entity instanceof EntityProjectileSpell projectile && ArsSpellAccess.resolver(projectile) != null)
            ActiveCasts.dispatched(ArsSpellAccess.resolver(projectile), success);
        return success;
    }
}
