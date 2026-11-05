package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.book.GlyphFormatter;
import dev.arsconflux.api.glyph.GlyphPresentation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Comparator;
import java.util.List;
import java.util.IdentityHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlyphFormatter.class, remap = false)
public abstract class GlyphFormatterOrderMixin {
    @Inject(method="buildSortedWidgets",at=@At("HEAD"))
    private void conflux$category(List<AbstractSpellPart> glyphs,CallbackInfo ci){dev.arsconflux.client.ProjectileGlyphCategory.prepare();}
    @WrapOperation(method = "buildSortedWidgets", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"))
    private void conflux$groupProviders(List<AbstractSpellPart> glyphs, Comparator<AbstractSpellPart> comparator, Operation<Void> original) {
        var categories = List.copyOf(GlyphFormatter.CATEGORIES);
        var groups = new IdentityHashMap<AbstractSpellPart, Integer>();
        for (var glyph : glyphs) {
            int group = categories.size() + glyph.getTypeIndex();
            // Later predicates specialize broad categories, without linking optional addon classes.
            for (int i = 0; i < categories.size(); i++)
                if (categories.get(i).filter().test(glyph)) group = i;
            groups.put(glyph, group);
        }
        original.call(glyphs, GlyphPresentation.withinCategories(glyphs, comparator, groups::get,
                dev.arsconflux.api.glyph.GlyphCategories::rank, group -> group));
    }
}
