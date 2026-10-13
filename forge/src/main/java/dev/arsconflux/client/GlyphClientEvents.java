package dev.arsconflux.client;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
@Mod.EventBusSubscriber(modid="ars_conflux",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class GlyphClientEvents {
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event){
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->GlyphGuiRenderer.reload());
    }
}
