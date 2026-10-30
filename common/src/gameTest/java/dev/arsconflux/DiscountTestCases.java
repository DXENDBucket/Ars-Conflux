package dev.arsconflux;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import dev.arsconflux.api.resource.*;
import dev.arsconflux.config.ConfluxConfig;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;


public final class DiscountTestCases {
 public record Balance(java.util.function.IntSupplier read, java.util.function.IntConsumer write) {
  int get(){return read.getAsInt();} void set(int value){write.accept(value);}
 }
 public static void run(GameTestHelper h,ServerPlayer p,Balance balance) {
  boolean enabled=ConfluxConfig.PERCENTAGE_DISCOUNT_RINGS.get(),billing=ConfluxConfig.VANILLA_TRIGGER_BILLING.get();
  double lesser=ConfluxConfig.LESSER_RING_REDUCTION.get(),greater=ConfluxConfig.GREATER_RING_REDUCTION.get();
  p.setGameMode(GameType.SURVIVAL);p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,3,2)));
  var inventory=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow(IllegalStateException::new);
  var ring=inventory.getCurios().get("ring");h.assertTrue(ring!=null&&ring.getStacks().getSlots()>=2,"Native ring slots are available");
  var previous0=ring.getStacks().getStackInSlot(0).copy();var previous1=ring.getStacks().getStackInSlot(1).copy();
  balance.set(10000);
  var source=new LivingCaster(p);
  var account=new ArsManaAccount(source);var spell=new Spell(MethodProjectile.INSTANCE,EffectHarm.INSTANCE);
  java.util.function.Supplier<SpellResolver> resolver=()->new SpellResolver(new SpellContext(h.getLevel(),spell,p,source,ItemStack.EMPTY));
  try {
   ConfluxConfig.PERCENTAGE_DISCOUNT_RINGS.set(true);ConfluxConfig.LESSER_RING_REDUCTION.set(.1);ConfluxConfig.GREATER_RING_REDUCTION.set(.2);ConfluxConfig.VANILLA_TRIGGER_BILLING.set(false);
   ring.getStacks().setStackInSlot(0,new ItemStack(ItemsRegistry.RING_OF_LESSER_DISCOUNT.get()));ring.getStacks().setStackInSlot(1,ItemStack.EMPTY);
   int raw=spell.getCost();h.assertTrue(resolver.get().getResolveCost()==ArsManaDiscounts.apply(raw,new java.math.BigDecimal("0.1")),"Native Ars lesser ring becomes ten percent, without its old flat ten");
   ring.getStacks().setStackInSlot(1,new ItemStack(ItemsRegistry.RING_OF_GREATER_DISCOUNT.get()));
   int full=ArsManaDiscounts.apply(raw,new java.math.BigDecimal("0.3"));
   h.assertTrue(dev.arsconflux.platform.ArsAccess.spentCost(resolver.get())==full,"Native ring percentages add once");
   var quote=new ResourceCharge(account,100);h.assertTrue(quote.amount()==70&&quote.originalAmount()==100,"Triggered account freezes the thirty-percent quote");
   balance.set(140);h.assertTrue(balance.get()==140,"Test mana balance initialized: "+balance.get());
   h.assertTrue(quote.affordableCount(5)==2,"Discounted volley budget: "+quote.affordableCount(5));
   h.assertTrue(quote.trigger(()->quote.trigger(()->true)),"Discounted nested reservation succeeds");
   h.assertTrue(balance.get()==0,"Two discounted debits: "+balance.get());
   balance.set(70);h.assertTrue(quote.trigger(()->{ring.getStacks().setStackInSlot(0,ItemStack.EMPTY);ring.getStacks().setStackInSlot(1,ItemStack.EMPTY);return true;})&&balance.get()==0,"Removing equipment during a trigger cannot change its reserved quote");
   ring.getStacks().setStackInSlot(0,new ItemStack(ItemsRegistry.RING_OF_LESSER_DISCOUNT.get()));ring.getStacks().setStackInSlot(1,new ItemStack(ItemsRegistry.RING_OF_GREATER_DISCOUNT.get()));
   ConfluxConfig.VANILLA_TRIGGER_BILLING.set(true);balance.set(10000);
   var deferred=resolver.get();int launch=dev.arsconflux.platform.ArsAccess.spentCost(deferred);h.assertTrue(deferred.onCast(ItemStack.EMPTY,h.getLevel()),"Native trigger-billed launch succeeds with converted rings");
   var target=h.spawn(EntityType.COW,8,3,2);target.setNoAi(true);deferred.onResolveEffect(h.getLevel(),new EntityHitResult(target));
   h.assertTrue(10000-balance.get()==full,"Native launch and one trigger together receive the percentage exactly once");
   int after=balance.get();deferred.onResolveEffect(h.getLevel(),new EntityHitResult(target));h.assertTrue(after-balance.get()==full-launch,"Repeated native effects use their own discounted slice");
   ConfluxConfig.PERCENTAGE_DISCOUNT_RINGS.set(false);ConfluxConfig.VANILLA_TRIGGER_BILLING.set(false);
   h.assertTrue(resolver.get().getResolveCost()==Math.max(0,raw-30)&&new ResourceCharge(account,100).amount()==100,"Disabling conversion restores native flat rings and undiscounted external trigger fees");
   ConfluxConfig.PERCENTAGE_DISCOUNT_RINGS.set(true);ConfluxConfig.GREATER_RING_REDUCTION.set(1.0);
   h.assertTrue(new ResourceCharge(account,1).amount()==0&&resolver.get().getResolveCost()==0,"One hundred percent is a deliberate complete reduction");h.succeed();
  } finally {
   ring.getStacks().setStackInSlot(0,previous0);ring.getStacks().setStackInSlot(1,previous1);p.discard();
   ConfluxConfig.PERCENTAGE_DISCOUNT_RINGS.set(enabled);ConfluxConfig.VANILLA_TRIGGER_BILLING.set(billing);ConfluxConfig.LESSER_RING_REDUCTION.set(lesser);ConfluxConfig.GREATER_RING_REDUCTION.set(greater);
  }
 }
}
