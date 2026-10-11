package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_allies") @PrefixGameTestTemplate(false)
public final class AlliesGameTests {
    @GameTest(template="empty",timeoutTicks=30) public static void optionalRegistration(GameTestHelper h){AlliesTestCases.registration(h);}
    @GameTest(template="empty",timeoutTicks=30) public static void vanillaAndProviderRelations(GameTestHelper h){AlliesTestCases.relations(h);}
    @GameTest(template="empty",timeoutTicks=30) public static void independentTargetsAndAttribution(GameTestHelper h){AlliesTestCases.fanout(h);}
}
