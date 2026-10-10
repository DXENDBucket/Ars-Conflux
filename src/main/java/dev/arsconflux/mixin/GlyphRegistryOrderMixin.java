package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.setup.registry.CreativeTabRegistry;
import dev.arsconflux.api.glyph.GlyphPresentation;
import java.util.Comparator;
import java.util.function.Function;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreativeTabRegistry.class, remap = false)
public abstract class GlyphRegistryOrderMixin {
    @Shadow public static Comparator<RecipeHolder<GlyphRecipe>> COMPARE_GLYPH_BY_TYPE;
    @Shadow public static Comparator<RecipeHolder<GlyphRecipe>> COMPARE_TYPE_THEN_NAME;
    @Shadow public static Comparator<RecipeHolder<GlyphRecipe>> COMPARE_TIER_THEN_NAME;
    @Shadow public static Comparator<AbstractSpellPart> COMPARE_SPELL_TYPE_NAME;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void conflux$groupProviders(CallbackInfo ci) {
        Function<RecipeHolder<GlyphRecipe>, AbstractSpellPart> part = holder -> holder.value().getSpellPart();
        COMPARE_GLYPH_BY_TYPE = GlyphPresentation.afterPeers(COMPARE_GLYPH_BY_TYPE, part, AbstractSpellPart::getTypeIndex);
        COMPARE_TYPE_THEN_NAME = GlyphPresentation.afterPeers(COMPARE_TYPE_THEN_NAME, part, AbstractSpellPart::getTypeIndex);
        COMPARE_TIER_THEN_NAME = GlyphPresentation.afterPeers(COMPARE_TIER_THEN_NAME, part, AbstractSpellPart::getTypeIndex);
        COMPARE_SPELL_TYPE_NAME = GlyphPresentation.afterPeers(COMPARE_SPELL_TYPE_NAME, Function.identity(), AbstractSpellPart::getTypeIndex);
    }
}
