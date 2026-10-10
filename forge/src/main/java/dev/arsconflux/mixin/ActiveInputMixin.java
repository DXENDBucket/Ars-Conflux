package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.ISpellCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.interaction.InputSessions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = com.hollingsworth.arsnouveau.api.spell.SpellCaster.class, remap = false, priority = 1100)
public abstract class ActiveInputMixin implements ISpellCaster {
    // Ars 4 implements this entry as an interface default; expose it on the concrete caster.
    @Override public InteractionResultHolder<ItemStack> castSpell(Level world, LivingEntity actor, InteractionHand hand,
                                                                 Component invalidMessage, Spell spell) {
        if (InputSessions.route(actor, hand, spell)) return InteractionResultHolder.consume(actor.getItemInHand(hand));
        return ISpellCaster.super.castSpell(world, actor, hand, invalidMessage, spell);
    }
}
