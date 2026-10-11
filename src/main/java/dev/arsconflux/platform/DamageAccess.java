package dev.arsconflux.platform;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;

/** Loader-specific Ars attribute and event-bus access. */
public final class DamageAccess {
    public static double bonus(LivingEntity caster) {
        return caster.getAttributes().hasAttribute(PerkAttributes.SPELL_DAMAGE_BONUS)
                ? caster.getAttributeValue(PerkAttributes.SPELL_DAMAGE_BONUS) : 0;
    }

    public static SpellDamageEvent.Pre pre(DamageSource source, LivingEntity caster, Entity target,
                                           float damage, SpellContext context) {
        var event = new SpellDamageEvent.Pre(source, caster, target, damage, context);
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    public static void post(DamageSource source, LivingEntity caster, Entity target,
                            float damage, SpellContext context) {
        NeoForge.EVENT_BUS.post(new SpellDamageEvent.Post(source, caster, target, damage, context));
    }

    private DamageAccess() {}
}
