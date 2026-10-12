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
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.STARTUP,
                dev.arsconflux.config.ConfluxStartupConfig.SPEC, "ars_conflux-startup.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,
                dev.arsconflux.config.ConfluxConfig.SPEC, "ars_conflux-server.toml");
        dev.arsconflux.glyph.OptionalGlyphs.register();
        BuiltinCarriers.register();
        var conditions = net.neoforged.neoforge.registries.DeferredRegister.create(
                net.neoforged.neoforge.registries.NeoForgeRegistries.CONDITION_SERIALIZERS, MOD_ID);
        conditions.register("elemental_phalanx_enabled", () -> dev.arsconflux.internal.elemental.PhalanxRecipeCondition.CODEC);
        conditions.register("nearby_allies_enabled", () -> dev.arsconflux.internal.content.NearbyAlliesCondition.CODEC);
        conditions.register(bus);
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            for (var profile : CarrierRegistry.profiles()) if (profile.glyph() != null)
                profile.rememberOriginalAugments(profile.glyph());
        }));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> { ExecutionSessions.tick(); dev.arsconflux.api.casting.ActiveCasts.tick(); });
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, (net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) ->
                dev.arsconflux.api.casting.PersistentCooldowns.copyOnClone(event.getOriginal(), event.getEntity(), event.isWasDeath()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> { ExecutionSessions.stop(); dev.arsconflux.api.casting.ActiveCasts.stop(); dev.arsconflux.api.damage.NativeSpellDamage.stop(); });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel world) { ExecutionSessions.unload(world); dev.arsconflux.api.casting.ActiveCasts.unload(world); dev.arsconflux.api.damage.NativeSpellDamage.unload(world); }
        });
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                (EntityJoinLevelEvent event) -> { if (ProjectileDispatch.convert(event.getEntity())) event.setCanceled(true); });
    }
}
