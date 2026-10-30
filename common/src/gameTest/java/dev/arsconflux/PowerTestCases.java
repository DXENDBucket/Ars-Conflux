package dev.arsconflux;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.damage.*;
import dev.arsconflux.api.school.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.config.ConfluxConfig;
import dev.arsconflux.platform.DamageAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import java.util.Map;

public final class PowerTestCases {
 private static final ResourceLocation SUN=ResourceLocation.tryParse("ars_conflux:sun"),DEATH=ResourceLocation.tryParse("ars_conflux:death");
 private static LivingEntity victim(GameTestHelper h,int x) {
  var entity=h.spawn(EntityType.COW,x,3,5);entity.setNoAi(true);entity.setNoGravity(true);
  entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);entity.setHealth(500);return entity;
 }
 private static float harm(GameTestHelper h,LivingEntity caster,LivingEntity target) {
  var context=SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE,EffectHarm.INSTANCE),caster,ItemStack.EMPTY);
  context.setCurrentIndex(1);new SpellResolver(context).onResolveEffect(h.getLevel(),new EntityHitResult(target));return 500-target.getHealth();
 }
 private static void close(double actual,double expected,GameTestHelper h,String label) {
  h.assertTrue(Math.abs(actual-expected)<.002,label+": "+actual+" != "+expected);
 }
 public static void run(GameTestHelper h) {
  boolean rules=ConfluxConfig.SCHOOL_DAMAGE_RULES.get(),billing=ConfluxConfig.VANILLA_TRIGGER_BILLING.get();
  var caster=DamageTestAccess.caster(h.getLevel());var frame=CastContext.detached(caster,null,null);
  var weights=SchoolWeights.of(Map.of(SUN,7,DEATH,3));var source=h.getLevel().damageSources().magic();
  var payload=h.spawn(EntityType.ARMOR_STAND,1,5,5);payload.setNoGravity(true);
  var child=new net.minecraft.world.entity.AreaEffectCloud(h.getLevel(),1,6,5);child.setNoGravity(true);
  try {
   ConfluxConfig.SCHOOL_DAMAGE_RULES.set(false);ConfluxConfig.VANILLA_TRIGGER_BILLING.set(false);
   double baseline=harm(h,caster,victim(h,1));h.assertTrue(baseline>0,"Native Ars baseline damages");
   PowerTestAccess.general(caster,.2);PowerTestAccess.school(caster,SUN,.5);
   close(SpellPower.multiplier(caster,SchoolWeights.EMPTY),1.2,h,"Undeclared receives general percentage");
   close(SpellPower.multiplier(caster,weights),1.55,h,"Weighted school bonus adds in same multiplier");
   close(harm(h,caster,victim(h,2)),baseline*1.55,h,"Native Ars damage gets exactly one combined multiplier");
   var target=victim(h,3);
   NativeSpellDamage.hurt(frame,weights,target,source,10,(s,n)->{close(n,24.8,h,"Common bridge retains +6 Ars flat bonus then power once");return true;});
   NativeSpellDamage.hurt(frame,SchoolWeights.EMPTY,target,source,10,(s,n)->{close(n,19.2,h,"Undeclared common bridge still gets general power");return true;});
   SchoolDamage.during(weights,()->CastContexts.scoped(frame,()->{
    var pre=DamageAccess.pre(source,caster,target,10,null);close(pre.damage,10,h,"Native pipeline defers scaling until final hurt");
    return target.hurt(pre.damageSource,pre.damage);
   }));
   close(500-target.getHealth(),15.5,h,"Iron/ANS-style Pre followed by native hurt is scaled once without extra flat damage");
   SchoolDamage.during(weights,()->CastContexts.scoped(frame,()->{
    SpellPower.hurt(caster,source,10,(s,n)->{close(n,15.5,h,"Self-targeted damage follows the same power formula");return true;});
    return SpellPower.handled(()->SpellPower.hurt(caster,source,10,(s,n)->{close(n,10,h,"Explicit native feedback costs remain unscaled");return true;}));
   }));
   var mundane=victim(h,4);mundane.hurt(source,10);close(500-mundane.getHealth(),10,h,"Cached ordinary magic source is not contaminated");
   var living=victim(h,5);
   SchoolDamage.during(weights,()->CastContexts.scoped(frame,()->{
    SpellPower.spawned(living);return null;
   }));h.assertTrue(SpellPower.profile(living)==null,"Ordinary summoned living entities do not gain a permanent melee multiplier");
   SpellPower.bind(payload,new SpellPower.Profile(caster,weights));
   SpellPower.tick(payload,()->h.getLevel().addFreshEntity(child));
   h.assertTrue(SpellPower.profile(child)!=null&&SpellPower.profile(child).schools().equals(weights),"Native emitter child inherits school identity");
   var delayed=victim(h,6);delayed.hurt(h.getLevel().damageSources().indirectMagic(child,caster),10);
   close(500-delayed.getHealth(),15.5,h,"Delayed source retains school identity outside effect scope");
   var emitter=victim(h,7);SpellPower.tick(child,()->emitter.hurt(source,10));
   close(500-emitter.getHealth(),15.5,h,"Native emitter damage using caster-only source inherits tick scope");
   var counter=victim(h,9);
   SchoolDamage.during(weights,()->CastContexts.scoped(frame,()->counter.hurt(h.getLevel().damageSources().mobAttack(living),10)));
   close(500-counter.getHealth(),10,h,"Unrelated attacker inside a spell callback does not borrow the caster's power");
   var sessionTarget=victim(h,8);
   var session=SchoolDamage.during(weights,()->new ExecutionSession<>(h.getLevel(),frame,0,60,new ExecutionDriver<Integer>(){
    public boolean start(ExecutionSession<Integer> s){return true;}
    public boolean tick(ExecutionSession<Integer> s){sessionTarget.hurt(source,10);return true;}
    public void close(ExecutionSession<Integer> s,ExecutionSession.EndReason reason){}
   },hit->{},()->true,ExecutionScope.DIRECT));
   h.assertTrue(session.start(),"Deferred execution starts");session.tick();
   close(500-sessionTarget.getHealth(),15.5,h,"Execution callbacks restore school snapshot and power caster");
   PowerTestAccess.general(caster,-2);PowerTestAccess.school(caster,SUN,2);
   close(SpellPower.multiplier(caster,weights),.4,h,"Bonuses below -100 percent offset positive school bonuses before clamping");
   PowerTestAccess.general(caster,-1);PowerTestAccess.school(caster,SUN,-1);
   close(SpellPower.multiplier(caster,weights),0,h,"Negative totals clamp to zero");
   h.assertTrue(SpellPower.current()==null&&!SchoolDamage.hasScope(),"No native damage context leaks");h.succeed();
  } finally {
   PowerTestAccess.general(caster,0);PowerTestAccess.school(caster,SUN,0);
   ConfluxConfig.SCHOOL_DAMAGE_RULES.set(rules);ConfluxConfig.VANILLA_TRIGGER_BILLING.set(billing);
   SpellPower.bind(payload,null);SpellPower.bind(child,null);payload.discard();child.discard();
  }
 }
}
