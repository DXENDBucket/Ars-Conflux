package dev.arsconflux.internal.zero;

import net.neoforged.fml.loading.LoadingModList;

/** Read discovery metadata without initializing optional spell, registry or client classes. */
public final class ZeroMixinSupport {
    private ZeroMixinSupport() {}
    public static boolean supported() {
        var mods = LoadingModList.get();
        return mods != null && mods.getMods().stream().anyMatch(m ->
                m.getModId().equals("ars_zero") && m.getVersion().toString().equals("2.0.2"));
    }
}
