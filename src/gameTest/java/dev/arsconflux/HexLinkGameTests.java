package dev.arsconflux;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_conflux_hex") @PrefixGameTestTemplate(false)
public final class HexLinkGameTests {
    @GameTest(template="empty", timeoutTicks=60)
    public static void independentProvidersUseNativeHexEntrypoints(GameTestHelper h) throws ReflectiveOperationException {
        h.assertTrue(!ModList.get().isLoaded("irons_nouveau") && !ModList.get().isLoaded("irons_spellbooks")
                && !ModList.get().isLoaded("goety"), "Core Hex integration requires no spell-content provider");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ConfluxHex"));
        player.setPos(h.absoluteVec(new Vec3(2, 3, 2))); player.setYRot(-90); player.setGameMode(GameType.SURVIVAL);
        var mana = com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player);
        mana.setMaxMana(10000);
        HexLinkCases.verify(h, player, ModList.get().isLoaded("hex_ars_link"), amount -> mana.setMana(amount));
    }
}
