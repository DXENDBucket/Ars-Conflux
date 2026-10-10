package dev.arsconflux.platform;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;

/** Ars version differences remain private to the loader implementation. */
public final class ArsAccess {
    private ArsAccess() {}
    public static SpellResolver resolver(EntityProjectileSpell carrier) { return carrier.resolver(); }
    public static void resolver(EntityProjectileSpell carrier, SpellResolver resolver) { carrier.setResolver(resolver); }
    public static boolean startsWithAugment(Spell spell) { return !spell.isEmpty() && spell.get(0) instanceof AbstractAugment; }
    public static boolean canSpend(com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster source, int amount) {
        return source.enoughMana(amount);
    }
    public static void spend(com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster source, int amount) {
        source.expendMana(amount);
    }
}
