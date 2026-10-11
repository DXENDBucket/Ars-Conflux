package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.ISpellCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.items.ScryCaster;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.casting.ActiveCasts;
import dev.arsconflux.api.casting.ActiveCastSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = com.hollingsworth.arsnouveau.api.spell.SpellCaster.class, remap = false)
public abstract class ActiveCasterChantMixin implements ISpellCaster {
    @Override public InteractionResultHolder<ItemStack> castSpell(Level world, LivingEntity caster, InteractionHand hand,
            Component invalidMessage, Spell spell) {
        return ActiveCasts.perform(spell, caster, hand, ActiveCastSource.CASTER_TOOL,
                () -> ISpellCaster.super.castSpell(world, caster, hand, invalidMessage, spell), InteractionResultHolder.fail(caster.getItemInHand(hand)),
                InteractionResultHolder.consume(caster.getItemInHand(hand)));
    }
}
