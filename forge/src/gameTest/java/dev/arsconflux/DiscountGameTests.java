package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_discounts") @PrefixGameTestTemplate(false)
public final class DiscountGameTests {
 @GameTest(template="empty",timeoutTicks=60,batch="discount_rings") public static void nativeRingsAndTriggeredQuotes(GameTestHelper h) {
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RingDiscount"));
  var mana=com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player).orElseThrow(IllegalStateException::new);
  mana.setMaxMana(10000);
  DiscountTestCases.run(h,player,new DiscountTestCases.Balance(()->(int)mana.getCurrentMana(),mana::setMana));
 }
}
