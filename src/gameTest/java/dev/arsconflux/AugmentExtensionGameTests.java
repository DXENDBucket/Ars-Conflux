package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("ars_conflux_augment_extensions") @PrefixGameTestTemplate(false)
public final class AugmentExtensionGameTests {
 @GameTest(template="empty",timeoutTicks=60) public static void blockAugments(GameTestHelper h){AugmentExtensionCases.blocks(h);}
 @GameTest(template="empty",timeoutTicks=60) public static void shockDuration(GameTestHelper h){AugmentExtensionCases.shock(h);}
 @GameTest(template="empty",timeoutTicks=40) public static void weightedPreciseDelay(GameTestHelper h){AugmentExtensionCases.precise(h);}
}
