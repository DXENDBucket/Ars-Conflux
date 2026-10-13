package dev.arsconflux.client;

import com.hollingsworth.arsnouveau.api.ArsNouveauAPI;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import dev.arsconflux.api.glyph.GlyphVisuals;
import dev.arsconflux.api.glyph.SpellValidators;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.glyph.MethodNearbyAllies;
import dev.arsconflux.internal.presentation.GlyphCandidateValidation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import java.util.*;

/** Opt-in client assertions and visual regression screenshot, excluded from release JARs. */
public final class GlyphStatePreview extends Screen {
    private record Row(String name, GlyphButton button, boolean dim) {}
    private final List<Row> rows = new ArrayList<>();
    private int frames;
    private static boolean opened;

    public static void openIfReady() {
        var mc = Minecraft.getInstance();
        if (!opened && Boolean.getBoolean("arsconflux.glyph_state_preview") && mc.screen != null && mc.getOverlay() == null) {
            opened = true;
            mc.setScreen(new GlyphStatePreview());
        }
    }

    private GlyphStatePreview() {
        super(Component.literal("Glyph candidate preview"));
        GlyphPreviewDefaults.load();
        // Force the actual client book class through Mixin transformation on both platforms.
        GuiSpellBook.class.getDeclaredMethods();
        var fireball = glyph("irons_nouveau:glyph_fireball");
        var icicle = glyph("irons_nouveau:glyph_icicle");
        var projectile = MethodProjectile.INSTANCE;
        var nativeHint = hint(projectile);
        GlyphCandidateValidation.scoped(() -> {
            check("Projectile + Fireball -> Icicle", List.of(projectile, fireball), icicle, true);
            check("Projectile + Fireball -> Amplify", List.of(projectile, fireball), AugmentAmplify.INSTANCE, false);
            check("Projectile -> Amplify", List.of(projectile), AugmentAmplify.INSTANCE, true);
            check("Projectile + Fireball + Harm -> Icicle", List.of(projectile, fireball, EffectHarm.INSTANCE), icicle, true);
            check("Projectile + Fireball + Harm -> Amplify", List.of(projectile, fireball, EffectHarm.INSTANCE), AugmentAmplify.INSTANCE, false);
            check("Imported duplicate payload -> Icicle", List.of(projectile, fireball, icicle), icicle, true);
            preservedMessages(projectile, fireball);
        });
        if (hint(projectile) != nativeHint) throw new AssertionError("Iron augment description was changed");
        if (nativeHint instanceof Component text && text.getContents() instanceof TranslatableContents translated
                && !translated.getKey().equals("irons_nouveau.augment.native_level"))
            throw new AssertionError("Iron native-level description replaced with generic text");
        if (!GlyphVisuals.applies(MethodNearbyAllies.INSTANCE)) throw new AssertionError("Nearby Allies has no frame");
        org.slf4j.LoggerFactory.getLogger("Ars Conflux preview").info("PASS: six glyph states, original validation text and Iron augment hint preserved; book mixin loaded");
    }

    private static AbstractSpellPart glyph(String id) {
        var part = GlyphRegistry.getSpellPart(CarrierRegistry.id(id));
        if (part == null) throw new AssertionError("Missing preview glyph " + id);
        return part;
    }

    private void check(String label, List<AbstractSpellPart> prefix, AbstractSpellPart candidate, boolean expectedDim) {
        var recipe = new ArrayList<>(prefix); recipe.add(candidate);
        var validator = ArsNouveauAPI.getInstance().getSpellCraftingSpellValidator();
        var errors = GlyphCandidateValidation.preview(validator, recipe, validator.validate(recipe));
        var applicable = errors.stream().filter(error -> error.getPosition() >= recipe.size() - 1).toList();
        boolean dim = !applicable.isEmpty();
        if (dim != expectedDim) throw new AssertionError(label + ": expected dim=" + expectedDim + ", actual=" + dim);
        var button = new GlyphButton(12, 24 + rows.size() * 32, candidate, ignored -> {});
        button.validationErrors.addAll(applicable);
        rows.add(new Row(label, button, dim));
        if (prefix.size() == 2 && candidate.getRegistryName().getPath().equals("glyph_icicle")) {
            var conflict = SpellValidators.validate(recipe).get(0);
            if (conflict.getPosition() != 0) throw new AssertionError("Runtime validation position changed");
        }
    }

    private static Object hint(AbstractSpellPart parent) {
        try { return ((Map<?, ?>) AbstractSpellPart.class.getField("augmentDescriptions").get(parent)).get(AugmentAmplify.INSTANCE); }
        catch (NoSuchFieldException absentInArs4) { return null; }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    private static void preservedMessages(AbstractSpellPart action, AbstractSpellPart candidate) {
        var existing = Component.translatable("irons_nouveau.validation.unsupported_modifier");
        var adding = Component.literal("native adding message");
        var original = new SpellValidationError() {
            public int getPosition() { return 0; }
            public AbstractSpellPart getSpellPart() { return action; }
            public MutableComponent makeTextComponentExisting() { return existing; }
            public MutableComponent makeTextComponentAdding() { return adding; }
        };
        ISpellValidator validator = recipe -> recipe.size() > 1 ? List.of(original) : List.of();
        var wrapped = GlyphCandidateValidation.preview(validator, List.of(action, candidate), List.of(original)).get(0);
        if (wrapped.getPosition() != 1 || wrapped.makeTextComponentExisting() != existing || wrapped.makeTextComponentAdding() != adding)
            throw new AssertionError("Original provider validation messages were not delegated");
    }

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        gui.fill(0, 0, width, height, 0xFF302F2B);
        gui.drawString(font, "Candidate glyph states (actual Ars GlyphButton)", 8, 8, 0xFFFFFFFF);
        for (var row : rows) {
            gui.pose().pushPose(); gui.pose().scale(1.5F, 1.5F, 1.5F);
            // The button owns the errors and calls Ars' original transparent-render path.
            var button = row.button(); int y = button.getY(); button.setY(Math.round(y / 1.5F));
            button.render(gui, -1, -1, partial); button.setY(y);
            gui.pose().popPose();
            gui.drawString(font, row.name() + (row.dim() ? " : dim" : " : bright"), 50, y + 4, 0xFFCCDDCC);
        }
        gui.renderItem(MethodNearbyAllies.INSTANCE.getGlyph().getDefaultInstance(), 12, 224);
        gui.drawString(font, "Nearby Allies: native Ars shape frame", 50, 228, 0xFFCCDDCC);
        gui.flush();
        if (++frames == 30) Screenshot.grab(minecraft.gameDirectory, "glyph-states.png", minecraft.getMainRenderTarget(), ignored -> {});
        if (frames == 90) minecraft.stop();
    }
}
