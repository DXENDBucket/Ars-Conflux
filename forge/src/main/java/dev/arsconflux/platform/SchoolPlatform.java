package dev.arsconflux.platform;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import dev.arsconflux.api.school.SchoolDamage;
import dev.arsconflux.internal.school.*;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class SchoolPlatform {
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event)->event.addListener(new SchoolReloadListener()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,(SpellDamageEvent.Pre event)->SchoolDamage.apply(event));
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event)->SchoolCommands.register(event.getDispatcher()));
    }
    private SchoolPlatform() {}
}
