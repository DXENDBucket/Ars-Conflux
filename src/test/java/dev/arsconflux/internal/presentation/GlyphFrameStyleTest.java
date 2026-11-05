package dev.arsconflux.internal.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlyphFrameStyleTest {
    private GlyphFrameStyle style(GlyphFrameStyle.Mode mode) {
        return new GlyphFrameStyle(true, true, .75, mode, 0x112233, 0x445566, 0x778899, 0xAABBCC);
    }
    @Test void nativeModeUsesArsColorsAndIgnoresCustomValues() {
        var s = style(GlyphFrameStyle.Mode.ARS_TYPE);
        assertEquals(0xB045E5, s.color(GlyphFrameStyle.Category.SHAPE));
        assertEquals(0xDC9613, s.color(GlyphFrameStyle.Category.AUGMENT));
        assertEquals(0x1C919A, s.color(GlyphFrameStyle.Category.EFFECT));
    }
    @Test void uniformAndCustomHaveIndependentPolicies() {
        for (var category : GlyphFrameStyle.Category.values())
            assertEquals(0x112233, style(GlyphFrameStyle.Mode.UNIFORM).color(category));
        var s = style(GlyphFrameStyle.Mode.CUSTOM);
        assertEquals(0x445566, s.color(GlyphFrameStyle.Category.SHAPE));
        assertEquals(0x778899, s.color(GlyphFrameStyle.Category.AUGMENT));
        assertEquals(0xAABBCC, s.color(GlyphFrameStyle.Category.EFFECT));
    }
    @Test void colorsRequireExactlySixHexDigits() {
        assertEquals(0xABCDEF, GlyphFrameStyle.parseColor("#aBcDeF"));
        assertEquals(0, GlyphFrameStyle.parseColor("000000"));
        for (var value : new Object[]{"#fff", "#11223344", "red", "0x112233", " #112233", 123})
            assertFalse(GlyphFrameStyle.validColor(value));
        assertThrows(IllegalArgumentException.class, () -> GlyphFrameStyle.parseColor("#GG0000"));
    }
    @Test void payloadDefaultsToCharcoalAndHasIndependentConfiguration(){
        assertEquals(0x202020,style(GlyphFrameStyle.Mode.ARS_TYPE).color(GlyphFrameStyle.Category.PROJECTILE));
        assertEquals(0x202020,style(GlyphFrameStyle.Mode.CUSTOM).color(GlyphFrameStyle.Category.PROJECTILE));
        var custom=new GlyphFrameStyle(true,true,.75,GlyphFrameStyle.Mode.ARS_TYPE,1,2,3,4,0x333333);
        assertEquals(0x333333,custom.color(GlyphFrameStyle.Category.PROJECTILE));
        assertEquals(0xDC9613,custom.color(GlyphFrameStyle.Category.AUGMENT));
    }
}
