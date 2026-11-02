package dev.arsconflux;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.resource.ResourceAccount;
import dev.arsconflux.api.targeting.Allies;
import dev.arsconflux.config.ConfluxStartupConfig;
import dev.arsconflux.glyph.PropagateMethod;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;

public final class FormPropagationCases {
    private static PropagateMethod part(String name){var p=GlyphRegistry.getSpellPart(CarrierRegistry.id("ars_conflux:glyph_propagate_"+name));return p instanceof PropagateMethod g?g:null;}
    private static LivingEntity cow(GameTestHelper h,int x){var c=h.spawn(EntityType.COW,x,2,2);c.setNoAi(true);c.setNoGravity(true);return c;}
    public static void registration(GameTestHelper h){
        boolean neg=dev.arsconflux.platform.ArsAccess.modLoaded("not_enough_glyphs")&&ConfluxStartupConfig.negFormPropagators();
        boolean allies=neg&&ConfluxStartupConfig.nearbyAllies();
        boolean phalanx=neg&&ConfluxStartupConfig.elementalPhalanx()&&GlyphRegistry.getSpellPart(CarrierRegistry.id("ars_elemental:glyph_carian_phalanx"))!=null;
        h.assertTrue((part("nearby_allies")!=null)==allies,"Nearby Allies propagation is conditional on NEG and its source form");
        h.assertTrue((part("carian_phalanx")!=null)==phalanx,"Phalanx propagation requires an available native form");
        for(var name:List.of("nearby_allies","carian_phalanx")){
            var p=part(name);var id=CarrierRegistry.id("ars_conflux:glyph_propagate_"+name);
            h.assertTrue(h.getLevel().getRecipeManager().byKey(id).isPresent()==(p!=null),"No dangling recipe when glyph is absent: "+name);
            if(p!=null)try{
                Class<?> api;try{api=Class.forName("com.alexthw.sauce.api.IPropagator");}catch(ClassNotFoundException absent){api=Class.forName("alexthw.not_enough_glyphs.api.IPropagator");}
                h.assertTrue(api.isInstance(p),"Native propagator marker for subform classification");
            }catch(ClassNotFoundException e){throw new RuntimeException(e);}
        }h.succeed();
    }
    public static void allies(GameTestHelper h){
        var part=part("nearby_allies");if(part==null){h.succeed();return;}
        var caster=cow(h,2);var friend=cow(h,4);var enemy=cow(h,6);var owner=cow(h,8);
        var account=new ResourceAccount(){public Object reservationKey(){return this;}public boolean canSpend(int n){return true;}public void spend(int n){}};
        var first=new Probe();var second=new Probe();var spell=new Spell(MethodTouch.INSTANCE,part,first,second);
        var ctx=new SpellContext(h.getLevel(),spell,caster,LivingCaster.from(caster),ItemStack.EMPTY);var resolver=new SpellResolver(ctx);
        var hit=new EntityHitResult(enemy);var frame=CastContext.of(ctx,hit,account).withDamageOwner(owner);CastContexts.bind(ctx,frame);ctx.nextPart();ctx.nextPart();
        try(var ignored=Allies.register(CarrierRegistry.id("test:propagators"),(a,b)->a==caster&&b==friend)){
            part.onResolve(hit,h.getLevel(),caster,new SpellStats.Builder().build(),ctx,resolver);
        }catch(Exception e){throw new RuntimeException(e);}
        h.assertTrue(ctx.isCanceled(),"NEG consumed the parent continuation");
        for(var probe:List.of(first,second)){
            h.assertTrue(probe.hits.equals(List.of(friend.getUUID())),"Each remaining effect runs once on the friend, not the trigger target");
            var got=probe.frames.get(0);h.assertTrue(got.caster()==caster&&got.damageOwner()==owner&&got.account()==account,"Native child retains attribution and payer");
        }h.succeed();
    }
    public static void phalanx(GameTestHelper h){
        var part=part("carian_phalanx");if(part==null){h.succeed();return;}
        var caster=cow(h,3);var target=cow(h,8);var effect=new Probe();var spell=new Spell(MethodTouch.INSTANCE,part,effect);
        var ctx=new SpellContext(h.getLevel(),spell,caster,LivingCaster.from(caster),ItemStack.EMPTY);var resolver=new SpellResolver(ctx);ctx.nextPart();ctx.nextPart();
        var stats=new SpellStats.Builder().setAugments(List.of(AugmentSplit.INSTANCE)).build();
        part.onResolve(new EntityHitResult(target),h.getLevel(),caster,stats,ctx,resolver);
        var shots=h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class,caster.getBoundingBox().inflate(12),e->e.getClass().getSimpleName().equals("EntityCarianPhalanx"));
        h.assertTrue(shots.size()==2,"Split creates two real Elemental formation projectiles, got "+shots.size());
        h.assertTrue(shots.stream().allMatch(e->e.getOwner()==caster),"Formation stays bound to original caster");
        h.assertTrue(shots.stream().allMatch(e->CarrierRegistry.of(e)!=null&&CarrierRegistry.of(e).glyphId().toString().equals("ars_elemental:glyph_carian_phalanx")),"Native formation children keep their real carrier identity");
        h.assertTrue(ctx.isCanceled(),"Parent does not execute the remaining effects twice");h.succeed();
    }
    private static final class Probe extends AbstractEffect{
        final List<UUID> hits=new ArrayList<>();final List<CastContext> frames=new ArrayList<>();
        Probe(){super(CarrierRegistry.id("test:propagate_probe"),"Probe");}
        public boolean isEnabled(){return true;}public int getDefaultManaCost(){return 0;}public SpellTier defaultTier(){return SpellTier.ONE;}
        public Set<AbstractAugment> getCompatibleAugments(){return Set.of();}
        public void onResolve(HitResult hit,Level world,LivingEntity caster,SpellStats stats,SpellContext ctx,SpellResolver resolver){hits.add(((EntityHitResult)hit).getEntity().getUUID());frames.add(CastContext.of(ctx,hit,null));}
    }
}
