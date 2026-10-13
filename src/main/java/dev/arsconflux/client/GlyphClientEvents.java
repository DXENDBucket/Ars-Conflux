package dev.arsconflux.client;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
@EventBusSubscriber(modid="ars_conflux",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class GlyphClientEvents {
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event){
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->GlyphGuiRenderer.reload());
    }
}
