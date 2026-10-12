package dev.arsconflux;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;

@GameTestHolder("ars_conflux_hex") @PrefixGameTestTemplate(false)
public final class HexLinkGameTests {
    private static HexLinkAdvancedCases.PlayerAccess player(GameTestHelper h, String name) {
        net.minecraft.server.level.ServerPlayer player;
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),name);
        if(name.equals("HexSource")||name.equals("HexReceiver")) {
            player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile);
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,player) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener) {}
            };
        }else player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),profile);
        player.setPos(h.absoluteVec(new Vec3(2, 3, 2))); player.setYRot(-90); player.setGameMode(GameType.SURVIVAL);
        var mana = com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player).orElseThrow(IllegalStateException::new);
        mana.setMaxMana(10000);
        return new HexLinkAdvancedCases.PlayerAccess(player, amount -> mana.setMana(amount), () -> mana.getCurrentMana());
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void hexPreparationAndCooldownOptionsAreIndependent(GameTestHelper h) throws ReflectiveOperationException {
        var access = player(h, "HexOptions");
        HexLinkCases.verifyOptions(h,access.player(),ModList.get().isLoaded("hex_ars_link"),access.setMana());
    }
    @GameTest(template="empty", timeoutTicks=40)
    public static void directHexRespectsActiveBoundary(GameTestHelper h) throws ReflectiveOperationException {
        var access = player(h, "HexActive");
        HexLinkCases.verifyActive(h, access.player(), ModList.get().isLoaded("hex_ars_link"), access.setMana(), false);
    }
    @GameTest(template="empty", timeoutTicks=40)
    public static void positionedHexRespectsActiveBoundary(GameTestHelper h) throws ReflectiveOperationException {
        var access = player(h, "HexLaunch");
        HexLinkCases.verifyActive(h, access.player(), ModList.get().isLoaded("hex_ars_link"), access.setMana(), true);
    }
    @GameTest(template="empty", timeoutTicks=40)
    public static void structuredRejectionChoosesLongestBlocker(GameTestHelper h) {
        var caster=h.spawn(net.minecraft.world.entity.EntityType.COW,2,2,2);
        var spell=new com.hollingsworth.arsnouveau.api.spell.Spell(com.hollingsworth.arsnouveau.common.spell.method.MethodSelf.INSTANCE);
        for(long ticks:new long[]{40,200}) {
            dev.arsconflux.api.casting.ActiveCasts.register(dev.arsconflux.api.projectile.CarrierRegistry.id("audit_fixture:"+java.util.UUID.randomUUID()),new dev.arsconflux.api.casting.ActiveCastProvider(){
                public boolean supports(com.hollingsworth.arsnouveau.api.spell.Spell s){return s==spell;}
                public dev.arsconflux.api.casting.CastRejection rejection(dev.arsconflux.api.casting.ActiveCastRequest request){return dev.arsconflux.api.casting.CastRejection.cooldown(net.minecraft.network.chat.Component.literal("Fixture"),ticks);}
            });
        }
        h.assertTrue(dev.arsconflux.api.casting.ActiveCasts.rejection(spell,caster,dev.arsconflux.api.casting.ActiveCastSource.CASTER_TOOL).remainingTicks()==200,"Common feedback selects the longest actual blocker");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void sourceBillingAndCallbackVm(GameTestHelper h) throws ReflectiveOperationException {
        var source = player(h, "HexSource");
        boolean loaded = ModList.get().isLoaded("hex_ars_link");
        if (loaded) {
            var enlightenment = h.getLevel().getServer().getAdvancements().getAdvancement(
                    dev.arsconflux.api.projectile.CarrierRegistry.id("hexcasting:enlightenment"));
            // Forge refuses awarding advancements to fake players; populate only this fixture's native progress.
            var progress = source.player().getAdvancements().getOrStartProgress(enlightenment);
            for (var criterion : enlightenment.getCriteria().keySet()) progress.grantProgress(criterion);
            h.assertTrue(progress.isDone(), "Delegation fixture has native enlightenment");
        }
        HexLinkAdvancedCases.verify(h, source, player(h, "HexReceiver"), loaded);
    }
    @GameTest(template="empty", timeoutTicks=60)
    public static void independentProvidersUseNativeHexEntrypoints(GameTestHelper h) throws ReflectiveOperationException {
        h.assertTrue(!ModList.get().isLoaded("irons_nouveau") && !ModList.get().isLoaded("irons_spellbooks")
                && !ModList.get().isLoaded("goety"), "Core Hex integration requires no spell-content provider");
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ConfluxHex"));
        player.setPos(h.absoluteVec(new Vec3(2, 3, 2))); player.setYRot(-90); player.setGameMode(GameType.SURVIVAL);
        var mana = com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player).orElseThrow(IllegalStateException::new);
        mana.setMaxMana(10000);
        HexLinkCases.verify(h, player, ModList.get().isLoaded("hex_ars_link"), amount -> mana.setMana(amount));
    }
}
