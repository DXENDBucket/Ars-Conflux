package dev.arsconflux.internal.presentation;

import java.util.*;
import java.util.function.Function;

/** A category starts a row; its icon shares that row with its first glyph. */
public final class CategoryGlyphPages {
    private CategoryGlyphPages() {}
    public record Cell<T>(T glyph,int row,int column) {}
    public record Heading<K>(K category,int row) {}
    public record Page<T,K>(List<Cell<T>> cells,List<Heading<K>> headings) {
        public Page { cells=List.copyOf(cells); headings=List.copyOf(headings); }
    }
    public static <T,K> List<Page<T,K>> arrange(List<T> sorted,int columns,int rows,
            Function<T,?> identity,Function<T,K> category) {
        if(columns<2 || rows<1)throw new IllegalArgumentException("A heading and a glyph must fit together");
        var pages=new ArrayList<Page<T,K>>();
        var cells=new ArrayList<Cell<T>>(); var headings=new ArrayList<Heading<K>>();
        var seen=new HashSet<>();
        K previous=null; int row=0,column=0; boolean first=true;
        for(var glyph:sorted) {
            if(!seen.add(identity.apply(glyph)))continue;
            K current=category.apply(glyph);
            boolean changed=first || !Objects.equals(previous,current);
            if(changed && column>0) { row++; column=0; }
            if(row>=rows) {
                pages.add(new Page<>(cells,headings)); cells.clear(); headings.clear();
                row=column=0;
            }
            if(changed || cells.isEmpty()) {
                headings.add(new Heading<>(current,row)); column=1;
            }
            cells.add(new Cell<>(glyph,row,column));
            if(++column==columns) { row++; column=0; }
            previous=current; first=false;
        }
        pages.add(new Page<>(cells,headings));
        return List.copyOf(pages);
    }
}
