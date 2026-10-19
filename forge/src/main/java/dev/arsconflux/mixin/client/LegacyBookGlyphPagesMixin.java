package dev.arsconflux.mixin.client;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.book.BaseBook;
import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import com.hollingsworth.arsnouveau.client.gui.book.InfinityGuiSpellBook;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import com.hollingsworth.arsnouveau.setup.registry.CreativeTabRegistry;
import dev.arsconflux.internal.presentation.LegacyGlyphPages;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ars 4 predates GlyphFormatter: ordinary and infinity books each own this layout. */
@Mixin(value = {GuiSpellBook.class, InfinityGuiSpellBook.class}, remap = false)
public abstract class LegacyBookGlyphPagesMixin extends BaseBook {
    @Shadow(remap = false) public List<AbstractSpellPart> displayedGlyphs;
    @Shadow(remap = false) public List<GlyphButton> glyphButtons;
    @Shadow(remap = false) public int page;
    @Shadow(remap = false) public int formTextRow;
    @Shadow(remap = false) public int augmentTextRow;
    @Shadow(remap = false) public int effectTextRow;
    @Shadow(remap = false) public PageButton nextButton;
    @Shadow(remap = false) public PageButton previousButton;
    @Shadow(remap = false) public abstract void onGlyphClick(Button button);

    @Unique private List<AbstractSpellPart> conflux$input;
    @Unique private Comparator<AbstractSpellPart> conflux$order;
    @Unique private List<LegacyGlyphPages.Page<AbstractSpellPart, String>> conflux$pages;

    @Unique
    private List<LegacyGlyphPages.Page<AbstractSpellPart, String>> conflux$pages() {
        if (conflux$pages == null || !displayedGlyphs.equals(conflux$input)
                || conflux$order != CreativeTabRegistry.COMPARE_TYPE_THEN_NAME) {
            conflux$input = new ArrayList<>(displayedGlyphs);
            conflux$order = CreativeTabRegistry.COMPARE_TYPE_THEN_NAME;
            var sorted = new ArrayList<>(displayedGlyphs);
            sorted.sort(conflux$order);
            conflux$pages = LegacyGlyphPages.arrange(sorted,
                    part -> part.getRegistryName() != null ? part.getRegistryName() : part,
                    part -> part instanceof AbstractCastMethod ? "form"
                            : part instanceof AbstractAugment ? "augment"
                            : part instanceof AbstractEffect ? "effect" : null);
        }
        return conflux$pages;
    }

    @Inject(method = "getNumPages", at = @At("HEAD"), cancellable = true)
    private void conflux$countPages(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(conflux$pages().size());
    }

    @Inject(method = "layoutAllGlyphs", at = @At("HEAD"), cancellable = true)
    private void conflux$layoutGlyphs(int requestedPage, CallbackInfo ci) {
        var pages = conflux$pages();
        page = Math.max(0, Math.min(requestedPage, pages.size() - 1));
        // Screen.removeWidget also removes narration entries; BaseBook.clearButtons does not.
        for (var button : glyphButtons) removeWidget(button);
        glyphButtons.clear();
        formTextRow = augmentTextRow = effectTextRow = 0;
        for (var cell : pages.get(page).cells()) {
            var button = new GlyphButton(bookLeft + 20 + cell.side() * 134 + cell.column() * 20,
                    bookTop + 20 + cell.row() * 18, cell.glyph(), this::onGlyphClick);
            addRenderableWidget(button);
            glyphButtons.add(button);
        }
        conflux$syncArrows();
        ci.cancel();
    }

    @Inject(method = "updateNextPageButtons", at = @At("HEAD"), cancellable = true)
    private void conflux$updateArrows(CallbackInfo ci) {
        conflux$syncArrows();
        ci.cancel();
    }

    @Unique
    private void conflux$syncArrows() {
        // init lays out glyphs before it creates the arrows. Native navigation still calls
        // layoutAllGlyphs and validate; synchronize again after its fixed-58 calculations.
        if (nextButton != null) nextButton.active = nextButton.visible = page + 1 < conflux$pages().size();
        if (previousButton != null) previousButton.active = previousButton.visible = page > 0;
    }

    @Inject(method = "drawBackgroundElements", at = @At("TAIL"))
    private void conflux$drawHeadings(GuiGraphics graphics, int mouseX, int mouseY,
                                     float partialTicks, CallbackInfo ci) {
        var pages = conflux$pages();
        for (var heading : pages.get(Math.max(0, Math.min(page, pages.size() - 1))).headings()) {
            graphics.drawString(font, Component.translatable("ars_nouveau.spell_book_gui." + heading.category()),
                    20 + heading.side() * 134, 23 + heading.row() * 18, -8355712, false);
        }
    }
}
