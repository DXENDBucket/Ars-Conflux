package dev.arsconflux.client;
/** The isolated menu preview has no server/world; load the same defaults used on connection. */
final class GlyphPreviewDefaults {
    static void load() { net.minecraftforge.fml.config.ConfigTracker.INSTANCE.loadDefaultServerConfigs(); }
}
