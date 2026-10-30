package dev.arsconflux.internal.ability;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.Map;

public final class AbilityReloadListener extends SimpleJsonResourceReloadListener {
    public AbilityReloadListener() {super(new GsonBuilder().create(),"conflux/glyph_abilities");}
    @Override protected void apply(Map<ResourceLocation,JsonElement> json,ResourceManager resources,ProfilerFiller profiler) {
        AbilityData.reload(json,message->LogUtils.getLogger().warn(message));
    }
}
