package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.common.items.ScryCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.interaction.InputSessions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ScryCaster.ScryCasterType.class, remap = false)
public abstract class ScryInputMixin {
    @Inject(method = "castSpell(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/network/chat/Component;Lcom/hollingsworth/arsnouveau/api/spell/Spell;)Lnet/minecraft/world/InteractionResultHolder;", at = @At("HEAD"), cancellable = true)
    private void arsConflux$input(Level world, LivingEntity actor, InteractionHand hand, Component invalidMessage,
                                 Spell spell, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci) {
        if (InputSessions.route(actor, hand, spell)) ci.setReturnValue(InteractionResultHolder.consume(actor.getItemInHand(hand)));
    }
}
