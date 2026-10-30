package dev.arsconflux;

import dev.arsconflux.api.execution.ExecutionSessions;
import dev.arsconflux.api.projectile.BuiltinCarriers;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.projectile.ProjectileDispatch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.server.level.ServerLevel;

@Mod(ArsConflux.MOD_ID)
public final class ArsConflux {
    public static final String MOD_ID = "ars_conflux";
    public ArsConflux() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                dev.arsconflux.config.ConfluxConfig.SPEC, "ars_conflux-server.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                dev.arsconflux.config.ConfluxStartupConfig.SPEC,"ars_conflux-startup.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                dev.arsconflux.config.ConfluxClientConfig.SPEC,"ars_conflux-client.toml");
        dev.arsconflux.platform.TimingAttributes.register(FMLJavaModLoadingContext.get().getModEventBus());
        dev.arsconflux.glyph.OptionalGlyphs.register();
        net.minecraftforge.common.crafting.CraftingHelper.register(new dev.arsconflux.internal.content.NearbyAlliesCondition.Serializer());
        BuiltinCarriers.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener((net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent event) -> event.enqueueWork(() -> {
            dev.arsconflux.internal.adams.AdamsArsPlusCompat.register();
            dev.arsconflux.internal.augments.OptionalAugmentExtensions.register();
            dev.arsconflux.api.glyph.AugmentEquivalence.refreshCompatibility();
        }));
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            for (var profile : CarrierRegistry.profiles()) if (profile.glyph() != null)
                profile.rememberOriginalAugments(profile.glyph());
        }));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) { ExecutionSessions.tick(); dev.arsconflux.api.casting.ActiveCasts.tick(); }
        });
        MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, (net.minecraftforge.event.entity.player.PlayerEvent.Clone event) ->
                dev.arsconflux.api.casting.PersistentCooldowns.copyOnClone(event.getOriginal(), event.getEntity(), event.isWasDeath()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> { ExecutionSessions.stop(); dev.arsconflux.api.casting.ActiveCasts.stop(); dev.arsconflux.api.damage.NativeSpellDamage.stop(); });
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel world) { ExecutionSessions.unload(world); dev.arsconflux.api.casting.ActiveCasts.unload(world); dev.arsconflux.api.damage.NativeSpellDamage.unload(world); }
        });
        MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST,
                (EntityJoinLevelEvent event) -> { if (ProjectileDispatch.convert(event.getEntity())) event.setCanceled(true); });
    }
}
