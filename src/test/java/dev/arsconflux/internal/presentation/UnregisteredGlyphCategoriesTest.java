package dev.arsconflux.internal.presentation;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import dev.arsconflux.api.glyph.GlyphPresentation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** NEG can specialize an effect into a subform even when no Conflux addon is installed. */
class UnregisteredGlyphCategoriesTest {
    @Test void categoryGroupingDoesNotRequireAnOptedInProvider() {
        assertFalse(GlyphPresentation.isProvider("unregistered_category_test"));
        AbstractSpellPart reverse = new Part("a_reverse", 5);
        AbstractSpellPart heal = new Part("b_heal", 1);
        AbstractSpellPart propagate = new Part("c_propagate", 5);
        AbstractSpellPart harm = new Part("d_harm", 1);
        var values = new ArrayList<>(List.of(reverse, heal, propagate, harm));
        values.sort(GlyphPresentation.withinCategories(values, Comparator.comparing(AbstractSpellPart::getName),
                p -> ((Part)p).category, p -> 5, category -> category));
        assertEquals(List.of(heal, harm, reverse, propagate), values);
    }
    private static final class Part extends AbstractSpellPart {
        final int category;
        Part(String name, int category) {
            super(ResourceLocation.fromNamespaceAndPath("unregistered_category_test", name), name);
            this.category = category;
        }
        @Override public Integer getTypeIndex() { return 5; }
        @Override protected int getDefaultManaCost() { return 0; }
        @Override protected Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
        @Override protected Set<SpellSchool> getSchools() { return Set.of(); }
        @Override public Component getTypeName() { return Component.empty(); }
    }
}
