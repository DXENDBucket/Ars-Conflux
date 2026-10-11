package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("ars_conflux_damage") @PrefixGameTestTemplate(false)
public final class DamageGameTests {
    @GameTest(template="empty",timeoutTicks=30) public static void attributeEventsCancellationAndFailure(GameTestHelper h){DamageTestCases.events(h);}
    @GameTest(template="empty",timeoutTicks=30) public static void sharedScopeGuardNestedScopesAndExceptionCleanup(GameTestHelper h){DamageTestCases.scopes(h);}
    @GameTest(template="empty",timeoutTicks=40) public static void transientAttachmentsAndDelayedChild(GameTestHelper h){DamageTestCases.attachments(h);}
}
