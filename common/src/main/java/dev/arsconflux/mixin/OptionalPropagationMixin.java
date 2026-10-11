package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.projectile.VolleyBudget;
import dev.arsconflux.api.projectile.CarrierRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Propagators bypass Ars' onCast: budget their own fan, not the still-active parent cast. */
@Pseudo
@Mixin(targets = {
        "alexthw.not_enough_glyphs.common.glyphs.propagators.PropagateProjectile",
        "alexthw.not_enough_glyphs.common.glyphs.propagators.PropagateMissile",
        "alexthw.not_enough_glyphs.common.glyphs.propagators.PropagateArc",
        "alexthw.not_enough_glyphs.common.glyphs.propagators.PropagateHoming",
        "alexthw.ars_elemental.common.glyphs.PropagatorArc",
        "alexthw.ars_elemental.common.glyphs.PropagatorHoming"
}, remap = false)
public abstract class OptionalPropagationMixin {
    @WrapMethod(method = "propagate")
    private void arsConflux$budget(Level world, HitResult hit, LivingEntity caster, SpellStats stats,
            SpellResolver resolver, Operation<Void> original) {
        CarrierRegistry.bind(resolver, ((AbstractSpellPart)(Object)this).getRegistryName());
        VolleyBudget.begin(resolver);
        try { original.call(world, hit, caster, stats, resolver); }
        finally { VolleyBudget.finish(resolver); }
    }
}
