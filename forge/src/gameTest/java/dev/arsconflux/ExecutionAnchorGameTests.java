package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_anchors") @PrefixGameTestTemplate(false)
public final class ExecutionAnchorGameTests {
    @GameTest(template="empty",timeoutTicks=30) public static void anchorsAndAuthority(GameTestHelper h) throws Exception{ExecutionAnchorCases.anchors(h);}
    @GameTest(template="empty",timeoutTicks=30) public static void claimsAndNestedCleanup(GameTestHelper h){ExecutionAnchorCases.claims(h);}
    @GameTest(template="empty",timeoutTicks=30) public static void directionReset(GameTestHelper h){ExecutionAnchorCases.reset(h);}
}
