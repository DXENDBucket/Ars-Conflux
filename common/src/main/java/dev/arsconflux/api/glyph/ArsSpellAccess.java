package dev.arsconflux.api.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.entity.EntitySpellArrow;
import dev.arsconflux.platform.ArsAccess;
import dev.arsconflux.mixin.SpellContextAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import java.util.List;

/** Source-compatible access to Ars 4/5 recipes and projectile resolvers. */
public final class ArsSpellAccess {
    private ArsSpellAccess() {}

    /** The stored world, without resolving a caster. May be null for a dehydrated context. */
    public static Level level(SpellContext context) {
        return ((SpellContextAccess)context).arsConflux$level();
    }

    /** Preserve Ars' server caster semantics. On clients/missing worlds only read the stored
     * entity; a device or network-only context may have none. Never create a client fake player. */
    public static LivingEntity caster(SpellContext context) {
        return level(context) instanceof ServerLevel ? context.getUnwrappedCaster()
                : ((SpellContextAccess)context).arsConflux$caster();
    }

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
