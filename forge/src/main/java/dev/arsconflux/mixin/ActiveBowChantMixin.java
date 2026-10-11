package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.common.items.SpellBow;
import com.hollingsworth.arsnouveau.common.items.SpellCrossbow;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.casting.ActiveCasts;
import dev.arsconflux.api.casting.ActiveCastSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/** Delay the original release/loading transaction as a whole, never individual arrows or their hits. */
@Mixin(value = {SpellBow.class, SpellCrossbow.class}, remap = false)
public abstract class ActiveBowChantMixin {
    @WrapMethod(method = "releaseUsing", remap = true)
    private void arsConflux$chant(ItemStack stack, Level world, LivingEntity entity, int timeLeft, Operation<Void> original) {
        var caster = stack.getItem() instanceof ICasterTool tool ? tool.getSpellCaster(stack) : null;
        var hand = entity.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (caster == null || entity.getItemInHand(hand) != stack) { original.call(stack, world, entity, timeLeft); return; }
        ActiveCasts.perform(caster.getSpell(), entity, hand, ActiveCastSource.CASTER_TOOL,
                () -> original.call(stack, world, entity, timeLeft), null, null);
    }
}
