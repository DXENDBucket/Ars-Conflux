package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.projectile.VolleyBudget;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Keep Elemental's formation, targeting and launch lifecycle on its original carrier. */
@Pseudo
@Mixin(targets = "alexthw.ars_elemental.common.glyphs.MethodCarianPhalanx", remap = false)
public abstract class OptionalElementalPhalanxMixin {
    @WrapMethod(method = "summonPhalanx")
    private void arsConflux$budget(Level world, LivingEntity caster, SpellStats stats,
            SpellContext context, SpellResolver resolver, Operation<Void> original) {
        VolleyBudget.begin(resolver);
        try { original.call(world, caster, stats, context, resolver); }
        finally { VolleyBudget.finish(resolver); }
    }

    // Elemental strips the original form when it builds the on-hit recipe. Bind the
    // child, not the parent, so subsequent propagators can declare their own form.
    @WrapOperation(method = "summonPhalanx", at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;getNewResolver(Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;)Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;"))
    private SpellResolver arsConflux$source(SpellResolver parent, SpellContext context,
            Operation<SpellResolver> original) {
        var child = original.call(parent, context);
        CarrierRegistry.bind(child, CarrierRegistry.id("ars_elemental:glyph_carian_phalanx"));
        return child;
    }
}
