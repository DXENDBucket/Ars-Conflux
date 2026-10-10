package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_interaction") @PrefixGameTestTemplate(false)
public final class InteractionGameTests {
    @GameTest(template="empty", timeoutTicks=25)
    public static void routingAndLease(GameTestHelper h) { InteractionCases.inputRoutingLeaseAndContext(h); }
    @GameTest(template="empty", timeoutTicks=25)
    public static void sourceIsolation(GameTestHelper h) { InteractionCases.cancellationAndSourceIsolation(h); }
    @GameTest(template="empty", timeoutTicks=25)
    public static void remainder(GameTestHelper h) { InteractionCases.continuationResumesOnlyRemainder(h); }
}
