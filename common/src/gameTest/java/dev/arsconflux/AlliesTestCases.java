package dev.arsconflux;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.resource.ResourceAccount;
import dev.arsconflux.api.targeting.Allies;
import dev.arsconflux.glyph.MethodNearbyAllies;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;

public final class AlliesTestCases {
    public static void registration(GameTestHelper h) {
        boolean expected=Boolean.getBoolean("arsconflux.test.allies_expected");
        var id=CarrierRegistry.id("ars_conflux:glyph_nearby_allies");
        h.assertTrue((GlyphRegistry.getSpellPart(id)!=null)==expected,"Startup controls glyph declaration");
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(id)==expected,"Startup controls physical glyph item");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(id).isPresent()==expected,"Startup controls the real Scribe recipe");
        h.assertTrue(MethodNearbyAllies.INSTANCE.defaultTier()==SpellTier.THREE,"Default glyph tier is three");
        h.succeed();
    }
    private static LivingEntity cow(GameTestHelper h,int x) {
        var entity=h.spawn(EntityType.COW,x,3,3);entity.setNoAi(true);entity.setNoGravity(true);return entity;
    }
    public static void relations(GameTestHelper h) {
        var caster=cow(h,2);var ally=cow(h,3);var stranger=cow(h,4);
        var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("allies_"+caster.getId());
        board.addPlayerToTeam(caster.getScoreboardName(),team);board.addPlayerToTeam(ally.getScoreboardName(),team);
        h.assertTrue(Allies.areAllies(caster,ally) && Allies.areAllies(ally,caster),"Vanilla team relation is symmetric");
        h.assertTrue(!Allies.areAllies(caster,stranger) && !Allies.areAllies(null,ally),"Neutral entities are not automatically allies");
        Wolf pet=h.spawn(EntityType.WOLF,5,3,3);pet.setOwnerUUID(caster.getUUID());
        Wolf other=h.spawn(EntityType.WOLF,6,3,3);other.setOwnerUUID(caster.getUUID());
        h.assertTrue(Allies.areAllies(caster,pet) && Allies.areAllies(pet,caster) && Allies.areAllies(pet,other),"Vanilla owner and shared owner are recognized");
        var key=CarrierRegistry.id("test:temporary_allies");
        try(var registration=Allies.register(key,(a,b)->a==caster && b==stranger)) {
            h.assertTrue(Allies.areAllies(stranger,caster),"Provider contributions are symmetric");
        } catch(Exception e) {throw new RuntimeException(e);}
        h.assertTrue(!Allies.areAllies(caster,stranger),"Closing removes provider contribution");
        board.removePlayerTeam(team);h.succeed();
    }
    public static void fanout(GameTestHelper h) {
        var caster=cow(h,2);var owner=cow(h,1);var near=cow(h,3);var far=cow(h,12);var enemy=cow(h,4);
        var center=caster.position();far.setPos(center.add(10,0,0));
        var first=new RecordEffect("one");var second=new RecordEffect("two");
        var spell=new Spell(MethodNearbyAllies.INSTANCE,first,second);
        var context=new SpellContext(h.getLevel(),spell,caster,LivingCaster.from(caster),ItemStack.EMPTY);
        ResourceAccount account=new ResourceAccount() {
            public Object reservationKey(){return this;}
            public boolean canSpend(int amount){return true;}
            public void spend(int amount){}
        };
        var frame=CastContext.of(context,null,account).withDamageOwner(owner);CastContexts.bind(context,frame);
        var resolver=new SpellResolver(context);context.nextPart();
        first.expected=caster;second.expected=caster;first.owner=owner;second.owner=owner;first.account=account;second.account=account;
        try(var registration=Allies.register(CarrierRegistry.id("test:fanout"),(a,b)->a==caster && (b==near || b==far))) {
            MethodNearbyAllies.INSTANCE.onCast(ItemStack.EMPTY,caster,h.getLevel(),new SpellStats.Builder().build(),context,resolver);
            h.assertTrue(first.targets.equals(List.of(near.getUUID())) && second.targets.equals(first.targets),"Both effects execute once on nearby ally only: first="+first.targets+", second="+second.targets+", near="+near.getUUID()+", index="+context.getCurrentIndex());
            first.targets.clear();second.targets.clear();
            var aoe=AugmentAOE.INSTANCE.applyModifiers(new SpellStats.Builder(),MethodNearbyAllies.INSTANCE).build();
            MethodNearbyAllies.INSTANCE.onCast(ItemStack.EMPTY,caster,h.getLevel(),aoe,context,resolver);
            h.assertTrue(first.targets.size()==2 && first.targets.contains(far.getUUID()) && second.targets.equals(first.targets),"AOE includes farther ally without consuming the sibling effect cursor");
            h.assertTrue(first.contexts.get(1)!=first.contexts.get(2),"Different target branches have distinct contexts");
            h.assertTrue(!first.targets.contains(caster.getUUID()) && !first.targets.contains(enemy.getUUID()),"Self and hostile/neutral targets are excluded");
        } catch(Exception e) {throw new RuntimeException(e);}
        h.succeed();
    }
    private static final class RecordEffect extends AbstractEffect {
        LivingEntity expected,owner;ResourceAccount account;
        final List<UUID> targets=new ArrayList<>();final List<SpellContext> contexts=new ArrayList<>();
        RecordEffect(String id){super(CarrierRegistry.id("test:"+id),id);}
        public int getDefaultManaCost(){return 0;}
        public boolean isEnabled(){return true;}
        public Set<AbstractAugment> getCompatibleAugments(){return Set.of();}
        public void onResolve(HitResult hit,Level world,LivingEntity caster,SpellStats stats,SpellContext context,SpellResolver resolver) {
            var frame=CastContext.of(context,hit,null);
            if(caster!=expected || frame.caster()!=expected || frame.damageOwner()!=owner || frame.account()!=account)
                throw new IllegalStateException("Target selection changed attribution or account");
            targets.add(((EntityHitResult)hit).getEntity().getUUID());contexts.add(context);
        }
    }
}
