package dev.arsconflux.internal.zero;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class ZeroHooks {
    private ZeroHooks() {}
    public static void register() { if(ZeroMixinSupport.supported()) NeoForge.EVENT_BUS.register(ZeroHooks.class); }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) { ZeroChannelCasts.tick(); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { ZeroChannelCasts.stop(); }
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) { ZeroChannelCasts.unload(event.getLevel()); }
}
