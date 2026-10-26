package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("ars_conflux_augment_providers") @PrefixGameTestTemplate(false)
public final class AugmentProviderGameTests {
 @GameTest(template="empty",timeoutTicks=150) public static void providerGrowth(GameTestHelper h) throws Exception {AugmentProviderCases.growth(h);}
}
