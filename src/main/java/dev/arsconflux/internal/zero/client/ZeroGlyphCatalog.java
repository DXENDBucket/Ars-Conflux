package dev.arsconflux.internal.zero.client;

import com.github.ars_zero.client.gui.StaffGuiTextures;
import com.hollingsworth.arsnouveau.api.documentation.DocAssets;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.client.gui.book.GlyphFormatter;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiImageButton;
import dev.arsconflux.api.glyph.GlyphPresentation;
import dev.arsconflux.internal.presentation.*;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Use Ars' actual category predicates, including optional addons' specializations. */
public final class ZeroGlyphCatalog {
    private ZeroGlyphCatalog() {}
    // Sauce is supplied by Zero; inspecting its marker does not add another compile/runtime dependency.
    private static final Class<?> PROPAGATOR=propagatorType();
    private static Class<?> propagatorType() {
        try { return Class.forName("com.alexthw.sauce.api.IPropagator",false,ZeroGlyphCatalog.class.getClassLoader()); }
        catch(ClassNotFoundException absent) { return null; }
    }
    private static boolean propagates(AbstractSpellPart part) { return PROPAGATOR!=null && PROPAGATOR.isInstance(part); }
    private static final GlyphFormatter.Category SUBFORM=new GlyphFormatter.Category(
            ResourceLocation.fromNamespaceAndPath("ars_nouveau","subform"),ZeroGlyphCatalog::propagates,
            Component.translatable("ars_nouveau.subform_icon_tooltip"),
            new DocAssets.BlitInfo(StaffGuiTextures.GLYPH_CATEGORY_PROPAGATE_FILTER,18,16));
    private static int rank(AbstractSpellPart part) { return part instanceof AbstractAugment?3:part.getTypeIndex(); }
    public static List<CategoryGlyphPages.Page<AbstractSpellPart,GlyphFormatter.Category>> pages(List<AbstractSpellPart> glyphs) {
        var categories=new ArrayList<>(GlyphFormatter.CATEGORIES);
        if(!categories.contains(SUBFORM))categories.add(SUBFORM);
        var fallback=categories.stream().filter(c->c.id().toString().equals("ars_nouveau:effect")).findFirst().orElse(SUBFORM);
        var groups=new IdentityHashMap<AbstractSpellPart,GlyphFormatter.Category>();
        var values=new ArrayList<AbstractSpellPart>();
        for(var part:glyphs) {
            if(part==null)continue;
            var selected=fallback;
            for(var definition:GlyphFormatter.CATEGORIES)if(definition.filter().test(part))selected=definition;
            // Zero also exposes subforms when no addon registered a specialized formatter category.
            if(propagates(part) && (selected==fallback))selected=SUBFORM;
            groups.put(part,selected); values.add(part);
        }
        Comparator<AbstractSpellPart> base=Comparator.comparingInt(ZeroGlyphCatalog::rank)
                .thenComparing(AbstractSpellPart::getLocaleName).thenComparing(p->p.getRegistryName().toString());
        var providerOrder=GlyphPresentation.withinCategories(values,base,groups::get,ZeroGlyphCatalog::rank,categories::indexOf);
        // Contiguity is required even without opt-in presentation providers.
        values.sort(DisplayOrder.categories(values,providerOrder,groups::get,ZeroGlyphCatalog::rank,categories::indexOf));
        return CategoryGlyphPages.arrange(values,14,5,AbstractSpellPart::getRegistryName,groups::get);
    }
    public static GuiImageButton heading(int x,int y,GlyphFormatter.Category category) {
        var texture=switch(category.id().toString()) {
            case "ars_nouveau:form" -> StaffGuiTextures.GLYPH_CATEGORY_FORM;
            case "ars_nouveau:augment" -> StaffGuiTextures.GLYPH_CATEGORY_AUGMENT;
            case "ars_nouveau:effect" -> StaffGuiTextures.GLYPH_CATEGORY_EFFECT;
            case "ars_nouveau:filter" -> StaffGuiTextures.GLYPH_CATEGORY_FILTER;
            case "ars_nouveau:subform" -> StaffGuiTextures.GLYPH_CATEGORY_PROPAGATE_FILTER;
            default -> null;
        };
        var button=texture==null?new GuiImageButton(x,y,category.blitInfo(),ignored->{})
                :new GuiImageButton(x,y,18,16,texture,ignored->{});
        return button.withTooltip(category.tooltip()).disableSound();
    }
}
