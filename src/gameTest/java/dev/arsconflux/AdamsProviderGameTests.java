package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("ars_conflux_adams_providers") @PrefixGameTestTemplate(false)
public final class AdamsProviderGameTests {
 @GameTest(template="empty",timeoutTicks=40) public static void ironAndGoety(GameTestHelper h) throws Exception {AdamsCompatibilityCases.providers(h);}
}
