package dev.arsconflux.internal.zero;

import java.util.List;

/** Weights verified against Zero 2.0.2's native SpellStats modifiers. */
public final class ZeroAugmentWeights {
    public record Definition(String glyph,String base,int units) {}
    public static List<Definition> definitions() {
        return List.of(new Definition("augment_amplify_two","glyph_amplify",2),
                new Definition("augment_amplify_three","glyph_amplify",4),
                new Definition("augment_aoe_two","glyph_aoe",2),
                new Definition("augment_aoe_three","glyph_aoe",4));
    }
    private ZeroAugmentWeights() {}
}
