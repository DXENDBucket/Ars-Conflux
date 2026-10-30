package dev.arsconflux.internal.school;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.Map;

public final class SchoolReloadListener extends SimpleJsonResourceReloadListener {
    public SchoolReloadListener() {super(new GsonBuilder().create(),"ars_conflux");}
    @Override protected void apply(Map<ResourceLocation,JsonElement> json,ResourceManager resources,ProfilerFiller profiler) {
        SchoolData.reload(json,message->LogUtils.getLogger().warn(message));
    }
}
