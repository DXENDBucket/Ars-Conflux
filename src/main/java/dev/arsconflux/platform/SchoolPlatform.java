package dev.arsconflux.platform;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import dev.arsconflux.api.school.SchoolDamage;
import dev.arsconflux.internal.school.*;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SchoolPlatform {
    public static void register() {
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event)->event.addListener(new SchoolReloadListener()));
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event)->event.addListener(new dev.arsconflux.internal.ability.AbilityReloadListener()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->dev.arsconflux.internal.ability.AbilityData.clear());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,(SpellDamageEvent.Pre event)->SchoolDamage.apply(event));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event)->SchoolCommands.register(event.getDispatcher()));
    }
    private SchoolPlatform() {}
}
