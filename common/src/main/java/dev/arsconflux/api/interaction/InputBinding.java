package dev.arsconflux.api.interaction;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/** Defines which active use belongs to an existing execution. Other sources may supply their own binding. */
public interface InputBinding {
    boolean matches(LivingEntity actor, InteractionHand hand, Spell spell);
    boolean valid();
    /** A recipe change on this source cancels the old endpoint before starting a new cast. */
    default boolean owns(LivingEntity actor, InteractionHand hand) { return false; }

    /** Bound to the held source and its recipe. Switching tools/slots never redirects stale input. */
    static InputBinding held(LivingEntity actor, InteractionHand hand, Spell spell) {
        Objects.requireNonNull(actor); Objects.requireNonNull(hand); Objects.requireNonNull(spell);
        ItemStack held = actor.getItemInHand(hand);
        ItemStack snapshot = held.copy();
        var recipe = ArsSpellAccess.parts(spell).stream().map(p -> p.getRegistryName()).toList();
        return new InputBinding() {
            public boolean valid() { return actor.getItemInHand(hand) == held && ItemStack.matches(held, snapshot); }
            public boolean owns(LivingEntity candidate, InteractionHand candidateHand) { return candidate == actor && candidateHand == hand; }
            public boolean matches(LivingEntity candidate, InteractionHand candidateHand, Spell candidateSpell) {
                return candidate == actor && candidateHand == hand && valid()
                        && recipe.equals(ArsSpellAccess.parts(candidateSpell).stream().map(p -> p.getRegistryName()).toList());
            }
        };
    }
}
