package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.common.items.SpellCrossbow;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsconflux.api.casting.ActiveCasts;
import dev.arsconflux.api.casting.ActiveCastSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SpellCrossbow.class, remap = false)
public abstract class CrossbowCooldownMixin {
    @WrapOperation(method = "shootOne", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private boolean arsConflux$spawn(Level world, net.minecraft.world.entity.Entity entity, Operation<Boolean> original) {
        boolean spawned = original.call(world, entity);
        ActiveCasts.arrowSpawned(entity, spawned);
        return spawned;
    }
    @WrapMethod(method = "shootStoredProjectiles")
    private void arsConflux$shoot(Level world, LivingEntity shooter, InteractionHand hand, ItemStack stack,
            float speed, float inaccuracy, Operation<Void> original) {
        var caster = stack.getItem() instanceof ICasterTool tool ? tool.getSpellCaster(stack) : null;
        if (caster == null) { original.call(world, shooter, hand, stack, speed, inaccuracy); return; }
        // Check before vanilla clears loaded ammunition. Preloaded extra crossbows cannot bypass shared cooldowns.
        ActiveCasts.execute(caster.getSpell(), shooter, ActiveCastSource.CASTER_TOOL,
                () -> original.call(world, shooter, hand, stack, speed, inaccuracy), null);
    }
}
