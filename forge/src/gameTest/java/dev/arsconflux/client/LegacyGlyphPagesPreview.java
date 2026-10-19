package dev.arsconflux.client;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.book.BaseBook;
import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import com.hollingsworth.arsnouveau.client.gui.book.InfinityGuiSpellBook;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import com.hollingsworth.arsnouveau.setup.registry.CreativeTabRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import sun.misc.Unsafe;

/** Test-only: exercise the transformed native book layouts with real registered glyphs.
 * Constructor bypass avoids requiring a world/player just to audit the book's widget grid.
 * Native gameplay validation and book constructors are deliberately outside this fixture.
 */
public final class LegacyGlyphPagesPreview extends Screen {
    private static boolean opened;
    private final List<AbstractSpellPart> glyphs;
    private final List<BaseBook> books = new ArrayList<>();
    private int frames;

    public static void openIfReady() {
        var mc = Minecraft.getInstance();
        if (!opened && Boolean.getBoolean("arsconflux.glyph_page_preview") && mc.screen != null && mc.getOverlay() == null) {
            opened = true;
            mc.setScreen(new LegacyGlyphPagesPreview());
        }
    }

    private LegacyGlyphPagesPreview() {
        super(Component.literal("Legacy Ars book pages"));
        GlyphPreviewDefaults.load();
        glyphs = new ArrayList<>(GlyphRegistry.getSpellpartMap().values().stream()
                .filter(AbstractSpellPart::shouldShowInSpellBook).sorted(CreativeTabRegistry.COMPARE_TYPE_THEN_NAME).toList());
        if (glyphs.size() < 200 || glyphs.stream().noneMatch(g -> g.getRegistryName().getNamespace().equals("not_enough_glyphs"))
                || glyphs.stream().noneMatch(g -> g.getRegistryName().getNamespace().equals("irons_nouveau")))
            throw new AssertionError("Expected a large real addon + NEG registry");
        // Mirror the Ars 4 constructor appending defaults already present in known glyphs.
        var withDuplicates = new ArrayList<>(glyphs);
        withDuplicates.addAll(GlyphRegistry.getDefaultStartingSpells());
        for (var type : List.of(GuiSpellBook.class, InfinityGuiSpellBook.class)) {
            var book = fixture(type);
            audit(book, withDuplicates);
            books.add(book);
        }
        org.slf4j.LoggerFactory.getLogger("Ars Conflux pages").info(
                "PASS: {} real glyphs in both native book classes; forward/backward layouts, duplicates, search subsets, arrows and widget cleanup", glyphs.size());
    }

    private static BaseBook fixture(Class<? extends BaseBook> type) {
        try {
            var singleton = Unsafe.class.getDeclaredField("theUnsafe");
            singleton.setAccessible(true);
            var book = (BaseBook) ((Unsafe) singleton.get(null)).allocateInstance(type);
            // Initialize only the GUI services/collections normally assigned by constructors.
            for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
                for (var field : owner.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    field.setAccessible(true);
                    if (List.class.isAssignableFrom(field.getType())) field.set(book, new ArrayList<>());
                    if (field.getType() == Font.class) field.set(book, Minecraft.getInstance().font);
                    if (field.getType() == Minecraft.class) field.set(book, Minecraft.getInstance());
                }
            }
            put(book, "nextButton", new PageButton(0, 0, true, ignored -> {}, true));
            put(book, "previousButton", new PageButton(0, 0, false, ignored -> {}, true));
            return book;
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private static void audit(BaseBook book, List<AbstractSpellPart> input) {
        put(book, "displayedGlyphs", input);
        int pages = (Integer) invoke(book, "getNumPages", new Class<?>[0]);
        var found = new ArrayList<AbstractSpellPart>();
        for (int p = 0; p < pages; p++) {
            layout(book, p);
            found.addAll(buttons(book).stream().map(b -> b.abstractSpellPart).toList());
            checkWidgets(book, p, pages);
        }
        var expected = input.stream().distinct().sorted(CreativeTabRegistry.COMPARE_TYPE_THEN_NAME).toList();
        if (!found.equals(expected)) throw new AssertionError("Missing, duplicate or reordered glyphs: " + book.getClass());
        for (int p = pages - 1; p >= 0; p--) { layout(book, p); checkWidgets(book, p, pages); }
        for (var subset : List.of(expected.stream().filter(g -> g.getRegistryName().getNamespace().equals("irons_nouveau")).toList(), List.<AbstractSpellPart>of())) {
            put(book, "displayedGlyphs", subset);
            layout(book, 0);
            int subsetPages = (Integer) invoke(book, "getNumPages", new Class<?>[0]);
            checkWidgets(book, 0, subsetPages);
            var filtered = new ArrayList<AbstractSpellPart>();
            for (int p = 0; p < subsetPages; p++) { layout(book, p); filtered.addAll(buttons(book).stream().map(b -> b.abstractSpellPart).toList()); }
            if (!filtered.equals(subset)) throw new AssertionError("Search subset lost glyphs");
        }
        put(book, "displayedGlyphs", input);
        layout(book, 0);
    }

    private static void checkWidgets(BaseBook book, int page, int pages) {
        var occupied = new HashSet<String>();
        for (var button : buttons(book)) {
            int x = button.getX() - book.bookLeft - 20, y = button.getY() - book.bookTop - 20;
            int side = x >= 134 ? 1 : 0, colX = x - side * 134;
            if (colX < 0 || colX > 100 || colX % 20 != 0 || y < 0 || y > 108 || y % 18 != 0
                    || !occupied.add(x + ":" + y)) throw new AssertionError("Outside or overlapping glyph button");
        }
        if (book.children().size() != buttons(book).size()) throw new AssertionError("Stale child widgets after paging");
        if (((List<?>) get(book, Screen.class, "narratables")).size() != buttons(book).size()) throw new AssertionError("Stale narration entries");
        var next = (PageButton) get(book, "nextButton");
        var previous = (PageButton) get(book, "previousButton");
        if (next.active != (page + 1 < pages) || next.visible != next.active
                || previous.active != (page > 0) || previous.visible != previous.active) throw new AssertionError("Incorrect arrows");
    }

    private static void put(Object object, String field, Object value) {
        try { object.getClass().getField(field).set(object, value); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static Object get(Object object, String field) {
        try { return object.getClass().getField(field).get(object); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static Object get(Object object, Class<?> owner, String field) {
        try { Field member = owner.getDeclaredField(field); member.setAccessible(true); return member.get(object); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static Object invoke(Object book, String method, Class<?>[] types, Object... args) {
        try { var member = book.getClass().getDeclaredMethod(method, types); member.setAccessible(true); return member.invoke(book, args); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    @SuppressWarnings("unchecked") private static List<GlyphButton> buttons(BaseBook book) { return (List<GlyphButton>) get(book, "glyphButtons"); }
    private static void layout(BaseBook book, int page) { invoke(book, "layoutAllGlyphs", new Class<?>[]{int.class}, page); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xFF302F2B);
        int index = Math.min(frames / 35, 3);
        var book = books.get(index / 2);
        book.bookLeft = width / 2 - 145;
        book.bookTop = height / 2 - 97;
        layout(book, index % 2);
        graphics.pose().pushPose();
        graphics.pose().translate(book.bookLeft, book.bookTop, 0);
        book.drawBackgroundElements(graphics, mouseX, mouseY, partial);
        graphics.pose().popPose();
        for (var button : buttons(book)) button.render(graphics, -1, -1, partial);
        graphics.drawString(font, book.getClass().getSimpleName() + " page " + (index % 2 + 1) + " / "
                + invoke(book, "getNumPages", new Class<?>[0]) + " (" + glyphs.size() + " unique glyphs)", 5, 5, 0xFFFFFFFF);
        graphics.flush();
        if (frames % 35 == 20) Screenshot.grab(minecraft.gameDirectory, "glyph-pages-" + index + ".png", minecraft.getMainRenderTarget(), ignored -> {});
        if (++frames == 140) minecraft.stop();
    }
}
