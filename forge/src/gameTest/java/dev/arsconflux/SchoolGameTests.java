package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_schools") @PrefixGameTestTemplate(false)
public final class SchoolGameTests {
 @GameTest(template="empty",timeoutTicks=80,batch="schools_only") public static void dataNativeArsDamageAndDelayedProfiles(GameTestHelper h) {SchoolTestCases.run(h);}
}
