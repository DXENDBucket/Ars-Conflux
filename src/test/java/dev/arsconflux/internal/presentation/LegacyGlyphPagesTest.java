package dev.arsconflux.internal.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyGlyphPagesTest {
    private record Glyph(int id, String category) {}

    private static List<LegacyGlyphPages.Page<Glyph, String>> arrange(List<Glyph> glyphs) {
        return LegacyGlyphPages.arrange(glyphs, Glyph::id, Glyph::category);
    }

    @Test void emptySearchHasOneEmptyPage() {
        var pages = arrange(List.of());
        assertEquals(1, pages.size());
        assertTrue(pages.get(0).cells().isEmpty());
        assertTrue(pages.get(0).headings().isEmpty());
    }

    @Test void knownAndDefaultGlyphsAreOnlyDisplayedOnce() {
        var glyphs = List.of(new Glyph(1, "form"), new Glyph(1, "form"),
                new Glyph(2, "form"), new Glyph(2, "form"));
        assertEquals(List.of(glyphs.get(0), glyphs.get(2)),
                arrange(glyphs).get(0).cells().stream().map(LegacyGlyphPages.Cell::glyph).toList());
    }

    @Test void exactCapacityDoesNotCreateAnEmptyTrailingPage() {
        // One heading consumes a row: 36 glyphs on the left and 42 on the right.
        var glyphs = IntStream.range(0, 78).mapToObj(i -> new Glyph(i, "effect")).toList();
        assertEquals(1, arrange(glyphs).size());
        var withOneMore = new ArrayList<>(glyphs);
        withOneMore.add(new Glyph(78, "effect"));
        var pages = arrange(withOneMore);
        assertEquals(2, pages.size());
        assertEquals(new LegacyGlyphPages.Cell<>(withOneMore.get(78), 0, 0, 0), pages.get(1).cells().get(0));
    }

    @Test void categoryChangesOnLaterPagesNeverReuseThePreviousRow() {
        var glyphs = new ArrayList<Glyph>();
        for (int i = 0; i < 140; i++) glyphs.add(new Glyph(i, i < 91 ? "augment" : "effect"));
        var pages = arrange(glyphs);
        var heading = pages.get(1).headings().get(0);
        var firstEffect = pages.get(1).cells().stream().filter(c -> c.glyph().id() == 91).findFirst().orElseThrow();
        assertEquals(heading.row() + 1, firstEffect.row());
        assertEquals(heading.side(), firstEffect.side());
        assertLayout(glyphs, pages);
    }

    @Test void headingsNearEitherPageEdgeStayWithTheirFirstGlyph() {
        for (int count : new int[]{30, 31, 35, 36, 37, 72, 73, 77, 78, 79}) {
            var glyphs = new ArrayList<Glyph>();
            for (int i = 0; i < count; i++) glyphs.add(new Glyph(i, "augment"));
            glyphs.add(new Glyph(count, "effect"));
            assertLayout(glyphs, arrange(glyphs));
        }
    }

    @Test void manyAddonsKeepEveryGlyphInOrderWithoutOverlapsOrLostRemainders() {
        for (int seed = 1; seed <= 20; seed++) {
            var glyphs = new ArrayList<Glyph>();
            for (int i = 0; i < 1500; i++) {
                var glyph = new Glyph(i, i < seed * 2 ? "form" : i < 450 + seed ? "augment" : "effect");
                glyphs.add(glyph);
                if (i % 7 == 0) glyphs.add(glyph);
            }
            assertLayout(glyphs, arrange(glyphs));
        }
    }

    private static void assertLayout(List<Glyph> glyphs, List<LegacyGlyphPages.Page<Glyph, String>> pages) {
        var found = new ArrayList<Glyph>();
        for (var page : pages) {
            var occupied = new HashSet<String>();
            for (var cell : page.cells()) {
                assertTrue(cell.side() >= 0 && cell.side() < 2);
                assertTrue(cell.row() >= 0 && cell.row() < 7);
                assertTrue(cell.column() >= 0 && cell.column() < 6);
                assertTrue(occupied.add(cell.side() + ":" + cell.row() + ":" + cell.column()));
                found.add(cell.glyph());
            }
            for (var heading : page.headings()) {
                assertTrue(heading.row() < 6, "A heading cannot be orphaned in the last row");
                for (int column = 0; column < 6; column++) {
                    assertTrue(occupied.add(heading.side() + ":" + heading.row() + ":" + column));
                }
                assertTrue(page.cells().stream().anyMatch(c -> c.side() == heading.side()
                        && c.row() == heading.row() + 1 && c.column() == 0
                        && c.glyph().category().equals(heading.category())));
            }
        }
        assertEquals(glyphs.stream().distinct().toList(), found);
    }
}
