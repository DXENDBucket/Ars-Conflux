package dev.arsconflux;

import dev.arsconflux.api.execution.ExecutionSessions;
import dev.arsconflux.api.projectile.BuiltinCarriers;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.projectile.ProjectileDispatch;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerLevel;

@Mod(ArsConflux.MOD_ID)
public final class ArsConflux {
    public static final String MOD_ID = "ars_conflux";
    public ArsConflux(IEventBus bus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,
                dev.arsconflux.config.ConfluxConfig.SPEC, "ars_conflux-server.toml");
        BuiltinCarriers.register();
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            for (var profile : CarrierRegistry.profiles()) if (profile.glyph() != null)
                profile.rememberOriginalAugments(profile.glyph());
        }));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> { ExecutionSessions.tick(); dev.arsconflux.api.casting.ActiveCasts.tick(); });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> { ExecutionSessions.stop(); dev.arsconflux.api.casting.ActiveCasts.stop(); });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel world) { ExecutionSessions.unload(world); dev.arsconflux.api.casting.ActiveCasts.unload(world); }
        });
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                (EntityJoinLevelEvent event) -> { if (ProjectileDispatch.convert(event.getEntity())) event.setCanceled(true); });
    }
}
