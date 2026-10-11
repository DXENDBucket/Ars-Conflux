package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_elemental") @PrefixGameTestTemplate(false)
public final class ElementalGameTests {
    @GameTest(template="empty", timeoutTicks=30)
    public static void generatedFansRetainElementalIdentityAndBudget(GameTestHelper h) { ElementalTestSupport.generatedFansRetainElementalIdentityAndBudget(h); }
}
