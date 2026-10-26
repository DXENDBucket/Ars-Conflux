package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_adams") @PrefixGameTestTemplate(false)
public final class AdamsCompatibilityGameTests {
 @GameTest(template="empty",timeoutTicks=60) public static void weightedAugments(GameTestHelper h){AdamsCompatibilityCases.augments(h);}
 @GameTest(template="empty",timeoutTicks=100) public static void domainContinuations(GameTestHelper h){AdamsCompatibilityCases.domain(h);}
 @GameTest(template="empty",timeoutTicks=40) public static void swappedContinuations(GameTestHelper h){AdamsCompatibilityCases.swap(h);}
}
