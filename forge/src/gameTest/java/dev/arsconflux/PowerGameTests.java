package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_power") @PrefixGameTestTemplate(false)
public final class PowerGameTests {
 @GameTest(template="empty",timeoutTicks=80,batch="power_only") public static void percentagePowerScopesAndNativePaths(GameTestHelper h) {PowerTestCases.run(h);}
}
