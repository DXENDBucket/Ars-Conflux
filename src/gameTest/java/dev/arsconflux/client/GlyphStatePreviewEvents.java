package dev.arsconflux.client;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid="ars_conflux", value=Dist.CLIENT)
public final class GlyphStatePreviewEvents {
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) { GlyphStatePreview.openIfReady(); }
}
