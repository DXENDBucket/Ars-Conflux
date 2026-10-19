package dev.arsconflux.client;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
@Mod.EventBusSubscriber(modid="ars_conflux", value=Dist.CLIENT)
public final class GlyphStatePreviewEvents {
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            GlyphStatePreview.openIfReady();
            LegacyGlyphPagesPreview.openIfReady();
        }
    }
}
