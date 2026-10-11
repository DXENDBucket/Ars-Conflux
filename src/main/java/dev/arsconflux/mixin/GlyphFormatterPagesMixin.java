package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.book.GlyphFormatter;
import com.hollingsworth.arsnouveau.client.gui.buttons.ANButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import dev.arsconflux.internal.presentation.GlyphPageLayout;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve upstream widgets and sorting; account for category padding before choosing a page. */
@Mixin(value=GlyphFormatter.class,remap=false)
public abstract class GlyphFormatterPagesMixin {
    @Shadow int perRow;
    @Shadow int maxRows;
    @Shadow int bookLeft;
    @Shadow int bookTop;
    @Shadow List<ANButton> sortedWidgets;
    @Shadow List<List<ANButton>> pages;
    @Shadow private void buildSortedWidgets(List<AbstractSpellPart> glyphs) {throw new AssertionError();}
    @Inject(method="buildPages",at=@At("HEAD"),cancellable=true)
    private void arsConflux$categoryBoundaries(List<AbstractSpellPart> glyphs,CallbackInfo ci) {
        // Do not assume another UI addon retained the standard grid dimensions.
        if(GlyphFormatter.MAX_PER_PAGE!=2*perRow*maxRows) return;
        buildSortedWidgets(glyphs);
        pages.clear();pages.add(new ArrayList<>());
        var layout=new GlyphPageLayout(perRow,maxRows,GlyphFormatter.MAX_PER_PAGE);
        for(var widget:sortedWidgets) {
            var slot=layout.next(!(widget instanceof GlyphButton));
            while(pages.size()<=slot.page()) pages.add(new ArrayList<>());
            widget.setX(bookLeft+16+slot.side()*134+slot.column()*18);
            widget.setY(bookTop+18+slot.row()*18);
            pages.get(slot.page()).add(widget);
        }
        ci.cancel();
    }
}
