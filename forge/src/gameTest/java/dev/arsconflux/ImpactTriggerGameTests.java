package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_impacts") @PrefixGameTestTemplate(false)
public final class ImpactTriggerGameTests {
 @GameTest(template="empty",timeoutTicks=30) public static void nativeDamageReceipts(GameTestHelper h){ImpactTriggerCases.receipts(h);}
 @GameTest(template="empty",timeoutTicks=30) public static void explicitActivations(GameTestHelper h){ImpactTriggerCases.activation(h);}
 @GameTest(template="empty",timeoutTicks=30) public static void nestingAndExceptions(GameTestHelper h){ImpactTriggerCases.nested(h);}
}
