package dev.arsconflux;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;

@GameTestHolder("ars_conflux_hex") @PrefixGameTestTemplate(false)
public final class HexLinkGameTests {
    private static HexLinkAdvancedCases.PlayerAccess player(GameTestHelper h, String name) {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name));
        player.setPos(h.absoluteVec(new Vec3(2, 3, 2))); player.setYRot(-90); player.setGameMode(GameType.SURVIVAL);
        var mana = com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player).orElseThrow(IllegalStateException::new);
        mana.setMaxMana(10000);
        return new HexLinkAdvancedCases.PlayerAccess(player, amount -> mana.setMana(amount), () -> mana.getCurrentMana());
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
