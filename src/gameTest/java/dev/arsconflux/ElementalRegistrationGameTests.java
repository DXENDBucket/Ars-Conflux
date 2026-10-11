package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.config.ConfluxStartupConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_conflux_registration") @PrefixGameTestTemplate(false)
public final class ElementalRegistrationGameTests {
    @GameTest(template="empty", timeoutTicks=30)
    public static void startupControlsProductionGlyphAndRecipe(GameTestHelper h) {
        boolean expected = Boolean.parseBoolean(System.getProperty("arsconflux.test.phalanx_expected", "true"));
        h.assertTrue(Boolean.getBoolean("arsconflux.test.production_seen"), "Test exercises Elemental's production gate");
        h.assertTrue(ConfluxStartupConfig.SPEC.isLoaded() && ConfluxStartupConfig.elementalPhalanx() == expected,
                "Startup file was loaded before Elemental constructed its registries");
        var id = CarrierRegistry.id("ars_elemental:glyph_carian_phalanx");
        h.assertTrue((GlyphRegistry.getSpellPart(id) != null) == expected, "Glyph registration follows the startup option");
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(id) == expected, "Physical glyph item registration follows the startup option");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(CarrierRegistry.id("ars_conflux:glyph_carian_phalanx")).isPresent() == expected,
                "Real scribing recipe loads only when enabled");
        h.succeed();
    }
}
