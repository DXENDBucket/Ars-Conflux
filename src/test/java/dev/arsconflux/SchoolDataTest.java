package dev.arsconflux;

import com.google.gson.JsonParser;
import dev.arsconflux.api.school.*;
import dev.arsconflux.internal.school.SchoolData;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SchoolDataTest {
    private static final net.minecraft.resources.ResourceLocation SUN = net.minecraft.resources.ResourceLocation.tryParse("ars_conflux:sun");
    private static final net.minecraft.resources.ResourceLocation DEATH = net.minecraft.resources.ResourceLocation.tryParse("ars_conflux:death");
    private static final ResourceLocation MOON = id("school_test:moon");

    private static ResourceLocation id(String value) {return ResourceLocation.tryParse(value);}
    @AfterEach void clear() {SchoolData.clear();}
    @Test void normalizedImmutableAndOptional() {
        assertTrue(SchoolRegistry.all().isEmpty(), "Core must not register preset schools");
        var input=new LinkedHashMap<ResourceLocation,Double>();input.put(SUN,7.0);input.put(DEATH,3.0);
        var weights=SchoolWeights.of(input);input.clear();
        assertEquals(0.7,weights.shares().get(SUN),1e-12);
        assertEquals(0.3,weights.shares().get(DEATH),1e-12);
        assertThrows(UnsupportedOperationException.class,()->weights.shares().clear());
        assertSame(SchoolWeights.EMPTY,SchoolWeights.of(Map.of()));
        assertTrue(SchoolWeights.of(Map.of(SUN,0)).isEmpty());
        assertEquals(1,SchoolWeights.EMPTY.multiplier(key->{throw new AssertionError("Undeclared weights queried a multiplier");}));
    }
    @Test void rejectsInvalidNumbersAndNormalizesHugeWeights() {
        for(double n:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY})assertThrows(IllegalArgumentException.class,()->SchoolWeights.of(Map.of(SUN,n)));
        var weights=SchoolWeights.of(Map.of(SUN,Double.MAX_VALUE,MOON,Double.MAX_VALUE));
        assertEquals(0.5,weights.shares().get(SUN));
        assertEquals(1.5,weights.multiplier(key->key.equals(SUN)?2:1));
        assertThrows(IllegalArgumentException.class,()->weights.multiplier(key->Double.NaN));
        assertThrows(IllegalArgumentException.class,()->SchoolWeights.fromJson(JsonParser.parseString("{\"test:sun\":\"7\"}").getAsJsonObject()));
    }
    @Test void weightedMultipliersAndJsonRoundTrip() {
        var weights=SchoolWeights.of(Map.of(SUN,7,DEATH,3));
        assertEquals(1.85,weights.multiplier(key->key.equals(SUN)?2:1.5),1e-12);
        var read=SchoolWeights.fromJson(weights.toJson());
        assertEquals(weights.shares().get(DEATH),read.shares().get(DEATH),1e-12);
    }
    @Test void datapackCanDefineCustomSchoolsAndOverrideCodeIncludingEmpty() {
        var glyph=id("school_test:custom_glyph");GlyphSchools.declare(glyph,SchoolWeights.of(SUN));
        var resources=new LinkedHashMap<ResourceLocation,com.google.gson.JsonElement>();
        resources.put(id("school_test:schools/dream"),JsonParser.parseString("{\"name\":\"Dream\",\"color\":\"#AABBCC\",\"ars_schools\":[\"conjuration\"]}"));
        resources.put(id("school_test:glyph_schools/custom_glyph"),JsonParser.parseString("{\"weights\":{\"school_test:dream\":2,\"ars_conflux:moon\":1}}"));
        SchoolData.reload(resources,message->fail(message));
        assertEquals(0xAABBCC,SchoolRegistry.get(id("school_test:dream")).orElseThrow().color());
        assertEquals(Set.of("conjuration"),SchoolRegistry.nativeSchoolIds(id("school_test:dream")));
        assertEquals(2.0/3,GlyphSchools.get(glyph).shares().get(id("school_test:dream")),1e-12);
        resources.put(id("school_test:glyph_schools/custom_glyph"),JsonParser.parseString("{\"weights\":{}}"));
        SchoolData.reload(resources,message->fail(message));assertTrue(GlyphSchools.get(glyph).isEmpty());
        SchoolData.clear();assertEquals(SchoolWeights.of(SUN),GlyphSchools.get(glyph));
        assertTrue(SchoolRegistry.get(id("school_test:dream")).isEmpty());
    }
    @Test void invalidResourcesDoNotPartiallyCorruptValidEntriesAndRulesAreOrdered() {
        var warnings=new ArrayList<String>();
        SchoolData.reload(Map.of(
            id("school_test:schools/bad"),JsonParser.parseString("{\"color\":-1}"),
            id("school_test:glyph_schools/bad"),JsonParser.parseString("{\"weights\":{\"ars_conflux:sun\":-1}}"),
            id("school_test:school_damage/z_low"),JsonParser.parseString("{\"school\":\"ars_conflux:sun\",\"multiplier\":2}"),
            id("school_test:school_damage/z_high"),JsonParser.parseString("{\"school\":\"ars_conflux:sun\",\"priority\":10,\"multiplier\":3}"),
            id("school_test:school_damage/a_high"),JsonParser.parseString("{\"school\":\"ars_conflux:sun\",\"priority\":10,\"multiplier\":4}")
        ),warnings::add);
        assertEquals(2,warnings.size());assertFalse(SchoolData.current().glyphs().containsKey(id("school_test:bad")));
        assertEquals(List.of("school_test:a_high","school_test:z_high","school_test:z_low"),SchoolData.current().rules().get(SUN).stream().map(rule->rule.id().toString()).toList());
    }
    @Test void scopeMaskingNestedExceptionsAndUnknownGlyphs() {
        var sun=SchoolWeights.of(SUN);assertTrue(GlyphSchools.get(id("school_test:undeclared")).isEmpty());
        SchoolDamage.during(sun,()->{
            assertEquals(sun,SchoolDamage.current());
            assertThrows(IllegalStateException.class,()->SchoolDamage.during(SchoolWeights.EMPTY,()->{assertTrue(SchoolDamage.current().isEmpty());throw new IllegalStateException();}));
            assertEquals(sun,SchoolDamage.current());return null;
        });assertTrue(SchoolDamage.current().isEmpty());
    }
}
