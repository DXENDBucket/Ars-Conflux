package dev.arsconflux;

import com.google.gson.JsonParser;
import dev.arsconflux.api.glyph.*;
import dev.arsconflux.internal.ability.AbilityData;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GlyphAbilitiesTest {
    private static ResourceLocation id(String text) {return ResourceLocation.tryParse(text);}
    @AfterEach void clear() {AbilityData.clear();}
    @Test void overridesAreAtomicImmutableAndDeletingRestoresDefaults() {
        var glyph=id("ability_test:heal");
        var input=new HashSet<>(Set.of(AbilityTags.HEALING,AbilityTags.ALLY));
        GlyphAbilities.declare(glyph,input);input.clear();
        assertTrue(GlyphAbilities.get(glyph).contains(AbilityTags.HEALING));
        AbilityData.reload(Map.of(glyph,JsonParser.parseString("{\"tags\":[]}")),Assertions::fail);
        assertTrue(GlyphAbilities.get(glyph).isEmpty());
        assertTrue(GlyphAbilities.declarations().containsKey(glyph));
        AbilityData.reload(Map.of(),Assertions::fail);
        assertEquals(Set.of(AbilityTags.HEALING,AbilityTags.ALLY),GlyphAbilities.get(glyph));
        assertThrows(UnsupportedOperationException.class,()->GlyphAbilities.get(glyph).clear());
    }
    @Test void unknownOptionalGlyphsAndCustomNamespacesRemainValid() {
        var absent=id("not_installed:custom");var tag=id("adventure:escape");
        AbilityData.reload(Map.of(absent,JsonParser.parseString("{\"tags\":[\"adventure:escape\"]}")),Assertions::fail);
        assertEquals(Set.of(tag),GlyphAbilities.get(absent));
        assertTrue(GlyphAbilities.get(id("ability_test:unknown")).isEmpty());
    }
    @Test void malformedOverridesNeverBecomeExplicitEmptyDeclarations() {
        var glyph=id("ability_test:defense");GlyphAbilities.declare(glyph,Set.of(AbilityTags.DEFENSE));
        var warnings=new ArrayList<String>();
        AbilityData.reload(Map.of(glyph,JsonParser.parseString("{\"tags\":[\"ars_conflux:defense\",2]}"),
                id("ability_test:valid"),JsonParser.parseString("{\"tags\":[\"ars_conflux:attack\"]}")),warnings::add);
        assertEquals(1,warnings.size());
        assertEquals(Set.of(AbilityTags.DEFENSE),GlyphAbilities.get(glyph));
        assertEquals(Set.of(AbilityTags.ATTACK),GlyphAbilities.get(id("ability_test:valid")));
        assertThrows(IllegalArgumentException.class,()->GlyphAbilities.declare(glyph,Set.of(AbilityTags.ATTACK)));
    }
}
