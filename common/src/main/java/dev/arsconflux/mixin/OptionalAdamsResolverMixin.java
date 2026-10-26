package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.arsconflux.api.context.ResolverContexts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** These effects construct a plain resolver instead of calling getNewResolver.
 * Retain source extensions while respecting AAP's cloned or explicitly exchanged caster context. */
@Pseudo
@Mixin(targets = {
        "com.adamsmods.adamsarsplus.glyphs.effect_glyph.EffectDomain",
        "com.adamsmods.adamsarsplus.glyphs.effect_glyph.EffectSwapTarget",
        "adamsmods.adamsarsplus.common.glyphs.effect_glyph.EffectDomain",
        "adamsmods.adamsarsplus.common.glyphs.effect_glyph.EffectSwapTarget"
}, remap = false)
public abstract class OptionalAdamsResolverMixin {
    @WrapOperation(method = "onResolve", at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;withSpell(Lcom/hollingsworth/arsnouveau/api/spell/Spell;)Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;"), require = 0)
    private SpellContext arsConflux$domainRemainder(SpellContext context, Spell remaining, Operation<SpellContext> original) {
        // AAP has already sliced the parent recipe. Its new continuation must start at zero;
        // AAP 6 makes child contexts again at each hit, otherwise slicing off the prefix twice.
        return original.call(context, remaining).resetCastCounter();
    }
    @WrapOperation(method = {"onResolve", "onResolveEntity"},
            at = @At(value = "NEW", target = "com/hollingsworth/arsnouveau/api/spell/SpellResolver"), require = 0)
    private SpellResolver arsConflux$inheritSource(SpellContext child, Operation<SpellResolver> original,
                                                  @Local(argsOnly = true) SpellResolver parent) {
        return ResolverContexts.derived(parent, original.call(child));
    }
}
