package dev.arsconflux.internal.presentation;

import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CategoryGlyphPagesTest {
    private record Glyph(int id,int category) {}
    private List<CategoryGlyphPages.Page<Glyph,Integer>> pages(List<Glyph> glyphs) {
        return CategoryGlyphPages.arrange(glyphs,14,5,Glyph::id,Glyph::category);
    }
    @Test void everyTypeStartsItsOwnRowWithNoOverlappingHeading() {
        var glyphs=IntStream.range(0,10).mapToObj(i->new Glyph(i,i/2)).toList();
        var result=pages(glyphs); assertEquals(1,result.size());
        var page=result.getFirst(); assertEquals(5,page.headings().size());
        for(int i=0;i<5;i++) {
            assertEquals(i,page.headings().get(i).row());
            assertEquals(1,page.cells().get(i*2).column());
            assertEquals(i,page.cells().get(i*2).row());
            assertEquals(2,page.cells().get(i*2+1).column());
        }
    }
    @Test void fullRowsAndPageBoundariesLoseNoGlyphsAndRepeatTheCategory() {
        var glyphs=IntStream.range(0,140).mapToObj(i->new Glyph(i,0)).toList();
        var result=pages(glyphs); assertEquals(List.of(69,69,2),result.stream().map(p->p.cells().size()).toList());
        assertEquals(glyphs,result.stream().flatMap(p->p.cells().stream()).map(CategoryGlyphPages.Cell::glyph).toList());
        for(var page:result) {
            assertEquals(1,page.headings().size()); assertEquals(0,page.headings().getFirst().row());
            assertEquals(1,page.cells().getFirst().column());
        }
    }
    @Test void manyTypesAndPartialRowsNeverShareCellsOrSkipItems() {
        var glyphs=new ArrayList<Glyph>(); int id=0;
        for(int type=0;type<30;type++)for(int i=0;i<type*3+1;i++)glyphs.add(new Glyph(id++,type));
        var result=pages(glyphs);
        assertEquals(glyphs,result.stream().flatMap(p->p.cells().stream()).map(CategoryGlyphPages.Cell::glyph).toList());
        for(var page:result) {
            var occupied=new HashSet<Integer>(); var rowTypes=new HashMap<Integer,Integer>();
            for(var heading:page.headings())assertTrue(occupied.add(heading.row()*14));
            for(var cell:page.cells()) {
                assertTrue(cell.row()>=0 && cell.row()<5 && cell.column()>=0 && cell.column()<14);
                assertTrue(occupied.add(cell.row()*14+cell.column()));
                assertEquals(cell.glyph().category(),rowTypes.computeIfAbsent(cell.row(),ignored->cell.glyph().category()));
            }
        }
    }
    @Test void emptySearchAndDuplicatedStartingGlyphsAreHandled() {
        assertEquals(1,pages(List.of()).size()); assertTrue(pages(List.of()).getFirst().cells().isEmpty());
        var duplicate=List.of(new Glyph(1,0),new Glyph(1,0),new Glyph(2,1));
        assertEquals(List.of(1,2),pages(duplicate).getFirst().cells().stream().map(c->c.glyph().id()).toList());
        assertThrows(IllegalArgumentException.class,()->CategoryGlyphPages.arrange(duplicate,1,5,Glyph::id,Glyph::category));
    }
}
