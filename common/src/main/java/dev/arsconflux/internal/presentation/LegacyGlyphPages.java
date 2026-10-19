package dev.arsconflux.internal.presentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Ars 4's two six-column pages, including the space occupied by category headings. */
public final class LegacyGlyphPages {
    public static final int COLUMNS = 6;
    public static final int ROWS = 7;

    private LegacyGlyphPages() {}

    public record Cell<T>(T glyph, int side, int row, int column) {}
    public record Heading<K>(K category, int side, int row) {}
    public record Page<T, K>(List<Cell<T>> cells, List<Heading<K>> headings) {
        public Page {
            cells = List.copyOf(cells);
            headings = List.copyOf(headings);
        }
    }

    /** Input is already sorted by Ars and the installed presentation providers. */
    public static <T, K> List<Page<T, K>> arrange(List<T> sorted,
                                                Function<T, ?> identity,
                                                Function<T, K> category) {
        var pages = new ArrayList<Page<T, K>>();
        var cells = new ArrayList<Cell<T>>();
        var headings = new ArrayList<Heading<K>>();
        var seen = new HashSet<>();
        int side = 0, row = 0, column = 0;
        K previous = null;
        boolean first = true;
        for (T glyph : sorted) {
            if (!seen.add(identity.apply(glyph))) continue;
            K current = category.apply(glyph);
            boolean newCategory = first || !Objects.equals(previous, current);
            if (newCategory && column > 0) {
                row++;
                column = 0;
            }
            // A heading must fit together with its first glyph, never on its own.
            int requiredRows = newCategory && current != null ? 2 : 1;
            if (row + requiredRows > ROWS) {
                side++;
                row = column = 0;
            }
            if (side == 2) {
                pages.add(new Page<>(cells, headings));
                cells.clear();
                headings.clear();
                side = 0;
            }
            if (newCategory && current != null) {
                headings.add(new Heading<>(current, side, row++));
            }
            cells.add(new Cell<>(glyph, side, row, column));
            if (++column == COLUMNS) {
                column = 0;
                row++;
            }
            previous = current;
            first = false;
        }
        // Empty searches are a single empty page, with both arrows disabled.
        pages.add(new Page<>(cells, headings));
        return List.copyOf(pages);
    }
}
