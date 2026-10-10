package dev.arsconflux.platform;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;

public final class ArsAccess {
    private ArsAccess() {}
    public static SpellResolver resolver(EntityProjectileSpell carrier) { return carrier.spellResolver; }
    public static void resolver(EntityProjectileSpell carrier, SpellResolver resolver) { carrier.spellResolver = resolver; }
    public static boolean startsWithAugment(Spell spell) { return !spell.isEmpty() && spell.recipe.get(0) instanceof AbstractAugment; }
    public static boolean canSpend(com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster source, int amount) {
        if (source instanceof com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster living)
            return com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(living.livingEntity)
                    .map(mana -> mana.getCurrentMana() >= amount).orElse(false);
        return false; // Ars 4 has no wrapped device-mana contract; devices supply a ResourceAccount.
    }
    public static void spend(com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster source, int amount) {
        if (source instanceof com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster living)
            com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(living.livingEntity)
                    .ifPresent(mana -> mana.removeMana(amount));
        else throw new IllegalStateException("Ars 4 device mana requires a provider ResourceAccount");
    }
}
