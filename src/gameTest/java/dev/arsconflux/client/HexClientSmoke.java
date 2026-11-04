package dev.arsconflux.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Finite menu-only smoke; never ships in the distribution or opens an existing world. */
@EventBusSubscriber(modid = "ars_conflux", value = Dist.CLIENT)
public final class HexClientSmoke {
    private static boolean done;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (done || !Boolean.getBoolean("arsconflux.hex_client_smoke")) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen == null || minecraft.getOverlay() != null) return;
        done = true;
        if (!ModList.get().isLoaded("hexcasting") || !ModList.get().isLoaded("hex_ars_link")
                || ModList.get().isLoaded("hex_pre53_client_fix"))
            throw new AssertionError("Use Hex/Linker without the standalone client patch");
        try {
            var service = Class.forName("at.petrak.hexcasting.xplat.IClientXplatAbstractions")
                    .getField("INSTANCE").get(null);
            if (service == null || !service.getClass().getName().equals("at.petrak.hexcasting.forge.xplat.ForgeClientXplatImpl"))
                throw new AssertionError("Missing native Hex client platform service");
            org.slf4j.LoggerFactory.getLogger("Ars Conflux").info("HEX_CLIENT_SMOKE_PASS: {}", service.getClass().getName());
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        minecraft.stop();
    }
}
