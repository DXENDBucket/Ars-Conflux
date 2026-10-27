package dev.arsconflux.mixin.client;

import com.github.ars_zero.client.gui.buttons.StaffArrowButton;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.book.SpellSlottedScreen;
import com.hollingsworth.arsnouveau.client.gui.book.GlyphFormatter;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import dev.arsconflux.config.ConfluxStartupConfig;
import dev.arsconflux.internal.presentation.CategoryGlyphPages;
import dev.arsconflux.internal.zero.client.ZeroGlyphCatalog;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Client-only replacement of grid accounting; native glyph actions/validation remain intact. */
@Pseudo
@Mixin(targets="com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen",remap=false)
public abstract class OptionalZeroGlyphLayoutMixin extends SpellSlottedScreen {
    @Shadow private List<GlyphButton> glyphButtons;
    @Shadow private List<Button> categoryButtons;
    @Shadow private List<AbstractSpellPart> displayedGlyphs;
    @Shadow private int page;
    @Shadow private StaffArrowButton nextButton;
    @Shadow private StaffArrowButton previousButton;
    @Shadow public abstract void onGlyphClick(Button button);
    @Unique private List<AbstractSpellPart> arsConflux$source;
    @Unique private int arsConflux$sourceSize;
    @Unique private List<CategoryGlyphPages.Page<AbstractSpellPart,GlyphFormatter.Category>> arsConflux$pages;
    protected OptionalZeroGlyphLayoutMixin(InteractionHand hand) { super(hand); }

    @Unique private List<CategoryGlyphPages.Page<AbstractSpellPart,GlyphFormatter.Category>> arsConflux$pages() {
        if(arsConflux$pages==null || arsConflux$source!=displayedGlyphs || arsConflux$sourceSize!=displayedGlyphs.size()) {
            arsConflux$source=displayedGlyphs; arsConflux$sourceSize=displayedGlyphs.size();
            arsConflux$pages=ZeroGlyphCatalog.pages(displayedGlyphs);
        }
        return arsConflux$pages;
    }
    @Unique private void arsConflux$arrows() {
        boolean back=page>0, next=page+1<arsConflux$pages().size();
        if(previousButton!=null) { previousButton.visible=back; previousButton.active=back; }
        if(nextButton!=null) { nextButton.visible=next; nextButton.active=next; }
    }
    @Inject(method="layoutAllGlyphs",at=@At("HEAD"),cancellable=true)
    private void arsConflux$layout(int requested,CallbackInfo ci) {
        if(!ConfluxStartupConfig.zeroFixes())return;
        if(glyphButtons.contains(getFocused()) || categoryButtons.contains(getFocused()))setFocused(null);
        for(var button:glyphButtons)removeWidget(button);
        for(var button:categoryButtons)removeWidget(button);
        glyphButtons.clear(); categoryButtons.clear();
        // Re-evaluate on native layout requests, including search, resize and page changes.
        arsConflux$pages=null;
        var pages=arsConflux$pages(); page=Math.max(0,Math.min(requested,pages.size()-1));
        var selected=pages.get(page); int x=bookLeft+87,y=bookTop+38;
        for(var heading:selected.headings()) {
            var button=ZeroGlyphCatalog.heading(x,y+18*heading.row(),heading.category());
            addRenderableWidget(button); categoryButtons.add(button);
        }
        for(var cell:selected.cells()) {
            var button=new GlyphButton(x+18*cell.column(),y+18*cell.row(),cell.glyph(),this::onGlyphClick);
            addRenderableWidget(button); glyphButtons.add(button);
        }
        arsConflux$arrows(); ci.cancel();
    }
    @Inject(method="getNumPages",at=@At("HEAD"),cancellable=true)
    private void arsConflux$pageCount(CallbackInfoReturnable<Integer> ci) {
        if(ConfluxStartupConfig.zeroFixes())ci.setReturnValue(arsConflux$pages().size());
    }
    @Inject(method="updateNextPageButtons",at=@At("HEAD"),cancellable=true)
    private void arsConflux$navigation(CallbackInfo ci) {
        if(ConfluxStartupConfig.zeroFixes()) { arsConflux$arrows(); ci.cancel(); }
    }
    @Inject(method="refreshCategoryButtons",at=@At("HEAD"),cancellable=true)
    private void arsConflux$keepGridHeadings(CallbackInfo ci) {
        if(ConfluxStartupConfig.zeroFixes())ci.cancel();
    }
}
