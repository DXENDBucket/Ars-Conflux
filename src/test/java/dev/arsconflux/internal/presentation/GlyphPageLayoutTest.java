package dev.arsconflux.internal.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlyphPageLayoutTest {
    @Test void categoryPaddingMovesEveryLastRowPositionToNextPage() {
        for(int filled=92;filled<98;filled++) {
            var layout=new GlyphPageLayout(7,7,98);
            for(int i=0;i<filled;i++) layout.next(false);
            var category=layout.next(true);
            assertEquals(new GlyphPageLayout.Slot(1,0,0,0),category);
            assertEquals(new GlyphPageLayout.Slot(1,0,1,0),layout.next(false));
        }
    }
    @Test void exactBoundaryAndColumnBoundaryKeepCategoryAndFirstGlyphTogether() {
        for(int boundary:new int[]{49,98,196}) {
            var layout=new GlyphPageLayout(7,7,98);
            for(int i=0;i<boundary;i++) layout.next(false);
            var category=layout.next(true);var glyph=layout.next(false);
            assertEquals(boundary/98,category.page());assertEquals(category.page(),glyph.page());
            assertEquals(category.row(),glyph.row());assertEquals(category.side(),glyph.side());
            assertEquals(0,category.column());assertEquals(1,glyph.column());
        }
    }
    @Test void manyAugmentsAndCategoriesNeverProduceOutsideOrDuplicateSlots() {
        var layout=new GlyphPageLayout(7,7,98);
        var seen=new java.util.HashSet<GlyphPageLayout.Slot>();
        for(int i=0;i<1500;i++) {
            var slot=layout.next(i%17==0 || i%131==0);
            assertTrue(slot.side()>=0 && slot.side()<2);assertTrue(slot.row()>=0 && slot.row()<7);
            assertTrue(slot.column()>=0 && slot.column()<7);assertTrue(seen.add(slot));
        }
    }
    @Test void ordinaryGlyphPositionsRemainTheNativeGrid() {
        var layout=new GlyphPageLayout(7,7,98);
        for(int i=0;i<300;i++) {
            var slot=layout.next(false);
            assertEquals(new GlyphPageLayout.Slot(i/98,(i%98)/49,i%7,(i/7)%7),slot);
        }
    }
}
