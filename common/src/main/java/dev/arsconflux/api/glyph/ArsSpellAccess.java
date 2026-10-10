package dev.arsconflux.api.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.entity.EntitySpellArrow;
import dev.arsconflux.platform.ArsAccess;
import java.util.List;

/** Source-compatible access to Ars 4/5 recipes and projectile resolvers. */
public final class ArsSpellAccess {
    private ArsSpellAccess() {}

    /** An immutable snapshot; modifying a recipe remains an Ars-version-specific operation. */
    public static List<AbstractSpellPart> parts(Spell spell) {
        return List.copyOf(ArsAccess.parts(spell));
    }

    public static SpellResolver resolver(EntityProjectileSpell projectile) {
        return ArsAccess.resolver(projectile);
    }

    public static void resolver(EntityProjectileSpell projectile, SpellResolver resolver) {
        ArsAccess.resolver(projectile, resolver);
    }

    public static SpellResolver resolver(EntitySpellArrow arrow) {
        return ArsAccess.resolver(arrow);
    }
}
