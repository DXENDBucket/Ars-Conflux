package dev.arsconflux.api.glyph;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A neutral GUI icon source. Calibration is relative to the 16px slot, before the user's center scale. */
public record GlyphIcon(ResourceLocation texture, ItemStack item, double scale, double offsetX, double offsetY) {
    public GlyphIcon {
        if ((texture == null) == (item == null)) throw new IllegalArgumentException("Specify exactly one icon source");
        if (item != null && item.isEmpty()) throw new IllegalArgumentException("Empty icon item");
        if (!Double.isFinite(scale) || scale <= 0 || scale > 4 || !Double.isFinite(offsetX) || !Double.isFinite(offsetY))
            throw new IllegalArgumentException("Invalid icon placement");
    }

    public static GlyphIcon texture(ResourceLocation texture) {
        return new GlyphIcon(java.util.Objects.requireNonNull(texture), null, 1, 0, 0);
    }
    public static GlyphIcon item(ItemStack stack) { return new GlyphIcon(null, stack.copy(), 1, 0, 0); }
    public GlyphIcon placed(double scale, double x, double y) { return new GlyphIcon(texture, item, scale, x, y); }
}
