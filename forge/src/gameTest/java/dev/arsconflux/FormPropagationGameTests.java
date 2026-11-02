package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_form_propagators") @PrefixGameTestTemplate(false)
public final class FormPropagationGameTests {
    @GameTest(template="empty") public static void registration(GameTestHelper h){FormPropagationCases.registration(h);}
    @GameTest(template="empty") public static void allies(GameTestHelper h){FormPropagationCases.allies(h);}
    @GameTest(template="empty") public static void phalanx(GameTestHelper h){FormPropagationCases.phalanx(h);}
}
