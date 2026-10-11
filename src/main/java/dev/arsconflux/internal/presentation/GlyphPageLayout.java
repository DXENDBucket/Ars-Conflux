package dev.arsconflux.internal.presentation;

/** Pure slot accounting for the two-column Ars book, including full-row category boundaries. */
public final class GlyphPageLayout {
    private final int columns,rows,capacity;
    private int cursor;
    public GlyphPageLayout(int columns,int rows,int capacity) {
        if(columns<=0 || rows<=0 || capacity!=2*columns*rows)
            throw new IllegalArgumentException("Expected two equal book columns");
        this.columns=columns;this.rows=rows;this.capacity=capacity;
    }
    public Slot next(boolean category) {
        if(category) cursor+=Math.floorMod(-cursor,columns);
        int page=cursor/capacity,local=cursor%capacity;
        cursor++;
        return new Slot(page,local/(columns*rows),local%columns,(local/columns)%rows);
    }
    public record Slot(int page,int side,int column,int row) {}
}
