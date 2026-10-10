package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.setup.registry.CreativeTabRegistry;
import dev.arsconflux.api.glyph.GlyphPresentation;
import java.util.Comparator;
import java.util.function.Function;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreativeTabRegistry.class, remap = false)
public abstract class GlyphRegistryOrderMixin {
    @Shadow public static Comparator<AbstractSpellPart> COMPARE_GLYPH_BY_TYPE;
    @Shadow public static Comparator<AbstractSpellPart> COMPARE_TYPE_THEN_NAME;
    @Shadow public static Comparator<AbstractSpellPart> COMPARE_TIER_THEN_NAME;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void conflux$groupProviders(CallbackInfo ci) {
        COMPARE_GLYPH_BY_TYPE = GlyphPresentation.afterPeers(COMPARE_GLYPH_BY_TYPE, Function.identity(), AbstractSpellPart::getTypeIndex);
        COMPARE_TYPE_THEN_NAME = GlyphPresentation.afterPeers(COMPARE_TYPE_THEN_NAME, Function.identity(), AbstractSpellPart::getTypeIndex);
        COMPARE_TIER_THEN_NAME = GlyphPresentation.afterPeers(COMPARE_TIER_THEN_NAME, Function.identity(), AbstractSpellPart::getTypeIndex);
    }
}
