package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_adams_absent") @PrefixGameTestTemplate(false)
public final class AdamsAbsentGameTests {
 @GameTest(template="empty",timeoutTicks=30) public static void optionalDependencyIsAbsent(GameTestHelper h){AdamsCompatibilityCases.absent(h);}
}
