package dev.arsconflux;
import dev.arsconflux.api.projectile.ImpactTriggers;
import dev.arsconflux.api.damage.NativeSpellDamage;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.*;
import net.minecraft.core.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ImpactTriggerCases {
 public static void receipts(GameTestHelper h) {
  var victim=h.spawn(EntityType.COW,1,3,1);victim.setNoAi(true);
  var other=h.spawn(EntityType.COW,3,3,1);var hit=new EntityHitResult(victim);
  var source=h.getLevel().damageSources().magic();var nativeCalls=new AtomicInteger();
  h.assertTrue(ImpactTriggers.resolve(hit,ImpactTriggers.Policy.ENTITY_DAMAGE,()->{nativeCalls.incrementAndGet();victim.hurt(source,2);}),"Raw native LivingEntity hurt authorizes its selected target");
  h.assertTrue(!ImpactTriggers.resolve(hit,ImpactTriggers.Policy.ENTITY_DAMAGE,()->{nativeCalls.incrementAndGet();victim.hurt(source,2);victim.setGlowingTag(true);}),"Equal rapid hits fail continuation during native invulnerability");
  h.assertTrue(nativeCalls.get()==2 && victim.isCurrentlyGlowing(),"Rejected continuation preserves native callback and secondary behavior");
  h.assertTrue(!ImpactTriggers.resolve(hit,ImpactTriggers.Policy.ENTITY_DAMAGE,()->other.hurt(source,2)),"Another blast victim cannot authorize the selected invulnerable target");
  victim.invulnerableTime=0;
  h.assertTrue(ImpactTriggers.resolve(hit,ImpactTriggers.Policy.ENTITY_DAMAGE,()->victim.hurt(source,2)),"Later accepted native hits can continue again");h.succeed();
 }
 public static void activation(GameTestHelper h) {
  var victim=h.spawn(EntityType.COW,1,3,1);var calls=new AtomicInteger();
  var block=new BlockHitResult(victim.position(),Direction.UP,victim.blockPosition(),false);
  h.assertTrue(ImpactTriggers.resolve(block,ImpactTriggers.Policy.ENTITY_DAMAGE,calls::incrementAndGet),"Block collision runs without requiring entity damage");
  h.assertTrue(ImpactTriggers.resolve(new EntityHitResult(victim),ImpactTriggers.Policy.ACTIVATION,()->victim.setNoGravity(true)),"Explicit status/field creation preserves its non-damage activation");
  h.assertTrue(!ImpactTriggers.resolve(new EntityHitResult(victim),ImpactTriggers.Policy.ENTITY_DAMAGE,()->{}),"Damage-required payload cannot fall back to empty collision");
  h.assertTrue(calls.get()==1 && victim.isNoGravity(),"Native activation occurs exactly once");h.succeed();
 }
 public static void nested(GameTestHelper h) {
  var a=h.spawn(EntityType.COW,1,3,1);var b=h.spawn(EntityType.COW,3,3,1);var hit=new EntityHitResult(a);var source=h.getLevel().damageSources().magic();
  boolean outer=ImpactTriggers.resolve(hit,ImpactTriggers.Policy.ENTITY_DAMAGE,()->NativeSpellDamage.hurt(null,a,source,2,(s,n)->{
    a.hurt(s,n);return false; // A native wrapper rejects an inner/reentrant attack.
  }));
  h.assertTrue(!outer,"Nested accepted hurt cannot bypass the outer damage function's rejection");
  a.invulnerableTime=0;
  h.assertTrue(ImpactTriggers.resolve(hit,ImpactTriggers.Policy.ENTITY_DAMAGE,()->{
    h.assertTrue(ImpactTriggers.resolve(new EntityHitResult(b),ImpactTriggers.Policy.ENTITY_DAMAGE,()->b.hurt(source,2)),"Nested impact has an independent receipt");
    a.hurt(source,2);
  }),"Nested observations restore the outer target scope");
  try {ImpactTriggers.resolve(hit,ImpactTriggers.Policy.ENTITY_DAMAGE,()->{throw new IllegalStateException("fixture");});}catch(IllegalStateException expected){}
  h.assertTrue(!ImpactTriggers.resolve(hit,ImpactTriggers.Policy.ENTITY_DAMAGE,()->{}),"Exceptional native callbacks cannot leak a damage receipt");h.succeed();
 }
}
