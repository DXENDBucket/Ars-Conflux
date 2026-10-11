package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("ars_conflux_optional") @PrefixGameTestTemplate(false)
public final class ControleAdditionsGameTests {
    @GameTest(template="empty", timeoutTicks=40)
    public static void realPrismResolvesItsDestinationAndKeepsNativeFallback(GameTestHelper h) throws Exception { ControleAdditionsCases.realPrismResolvesItsDestinationAndKeepsNativeFallback(h); }
    @GameTest(template="empty", timeoutTicks=40)
    public static void transferKeepsInstancesSourcePaymentAndContinuation(GameTestHelper h) { ControleAdditionsCases.transferKeepsInstancesSourcePaymentAndContinuation(h); }
    @GameTest(template="empty", timeoutTicks=40)
    public static void transferFailureAndOwnerDepartureCloseCleanly(GameTestHelper h) { ControleAdditionsCases.transferFailureAndOwnerDepartureCloseCleanly(h); }
    @GameTest(template="empty", timeoutTicks=40)
    public static void retaliationUsesCurrentDamageAndRestoresNestedScopes(GameTestHelper h) { ControleAdditionsCases.retaliationUsesCurrentDamageAndRestoresNestedScopes(h); }
}
