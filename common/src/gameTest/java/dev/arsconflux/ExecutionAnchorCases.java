package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.targeting.Allies;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExecutionAnchorCases {
    private static LivingEntity cow(GameTestHelper h,int x){var e=h.spawn(EntityType.COW,x,3,3);e.setNoAi(true);e.setNoGravity(true);e.setYRot(0);return e;}
    public static void anchors(GameTestHelper h) throws Exception {
        var root=cow(h,1);var ally=cow(h,3);var credit=cow(h,5);
        var frame=CastContext.detached(root,new EntityHitResult(ally),null).withDamageOwner(credit).withIncomingDirection(new Vec3(1,0,0));
        var registry=Allies.register(ResourceLocation.tryParse("test:anchor"),(a,b)->a==root && b==ally);
        try {
            var anchor=ExecutionAnchor.beneficial(frame,ExecutionAnchor.Basis.EYES,new Vec3(0,-.8,0));
            h.assertTrue(anchor!=null && anchor.entity()==ally && anchor.sample().direction().z>.99,"Provider-only ally accepted; entity facing wins over incoming geometry");
            ally.setYRot(90);ally.setPos(ally.position().add(0,1,0));
            h.assertTrue(anchor.sample().origin().distanceTo(ally.getEyePosition().add(0,-.8,0))<.001 && anchor.sample().direction().x<-.99,"Live position and aim follow actual entity");
            h.assertTrue(ExecutionAnchor.beneficial(frame.withTarget(new EntityHitResult(credit)),ExecutionAnchor.Basis.FEET,Vec3.ZERO)==null,"Damage credit does not authorize a hostile executor");
            var controlled=ExecutionAnchor.entity(credit,ExecutionAnchor.Basis.FEET,Vec3.ZERO);
            h.assertTrue(controlled.present(h.getLevel()) && controlled.valid(h.getLevel(),e->e==credit)
                    && !controlled.valid(h.getLevel(),frame),"Neutral anchor permits explicit provider authorization without weakening beneficial defaults");
            var point=root.position().add(2,0,2);
            var fixed=ExecutionAnchor.beneficial(frame.withTarget(new BlockHitResult(point,Direction.UP,BlockPos.containing(point),false)),ExecutionAnchor.Basis.EYES,new Vec3(0,-.8,0));
            root.setYRot(180);
            h.assertTrue(fixed.entity()==null && fixed.sample().origin().equals(point) && fixed.sample().direction().x>.99,"Position is exact, frozen, independent of entity offsets and camera");
            registry.close();
            h.assertTrue(!anchor.valid(h.getLevel(),frame) && fixed.valid(h.getLevel(),frame),"Loss of authorization invalidates only the entity anchor");
            h.assertTrue(ExecutionAnchor.beneficial(frame.withTarget(BlockHitResult.miss(point,Direction.UP,BlockPos.containing(point))),ExecutionAnchor.Basis.FEET,Vec3.ZERO)==null,"MISS is not a world execution origin");
            h.succeed();
        }finally{registry.close();root.discard();ally.discard();credit.discard();}
    }
    private static ExecutionSession<Integer> session(GameTestHelper h,CastContext frame,List<ExecutionClaim> claims,AtomicInteger starts,Runnable onStart) {
        return new ExecutionSession<>(h.getLevel(),frame,0,30,new ExecutionDriver<Integer>() {
            public List<ExecutionClaim> claims(ExecutionSession<Integer> s){return claims;}
            public boolean start(ExecutionSession<Integer> s){starts.incrementAndGet();onStart.run();return true;}
            public boolean tick(ExecutionSession<Integer> s){return false;}
            public void close(ExecutionSession<Integer> s,ExecutionSession.EndReason reason){}
        },hit->{},()->true,ExecutionScope.DIRECT);
    }
    public static void claims(GameTestHelper h) {
        var root=cow(h,1);var actor=cow(h,3);var frame=CastContext.detached(root,null,null).withExecutor(actor);
        var starts=new AtomicInteger();var sessions=new ArrayList<ExecutionSession<Integer>>();
        try {
            var nested=session(h,frame,List.of(ExecutionClaim.action(actor)),starts,()->{});sessions.add(nested);
            var action=session(h,frame,List.of(ExecutionClaim.action(actor)),starts,()->h.assertTrue(!ExecutionSessions.start(nested),"Nested driver start sees the parent's claim"));sessions.add(action);
            h.assertTrue(ExecutionSessions.start(action) && starts.get()==1,"Only the parent exclusive action starts");
            h.assertTrue(!ExecutionSessions.start(action) && action.active(),"Duplicate start cannot cancel an already active session");
            h.assertTrue(ExecutionSessions.isCasting(actor) && !ExecutionSessions.isCasting(root),"Actual executor is occupied, payer is not");
            var one=session(h,frame,List.of(ExecutionClaim.channel(actor)),starts,()->{});var two=session(h,frame,List.of(ExecutionClaim.channel(actor)),starts,()->{});sessions.add(one);sessions.add(two);
            h.assertTrue(ExecutionSessions.start(one) && ExecutionSessions.start(two),"Shared channels coexist and do not collide with physical actions");
            one.finish(ExecutionSession.EndReason.COMPLETED);
            h.assertTrue(ExecutionSessions.isOccupied(actor,ExecutionClaim.CHANNELING),"Ending one session does not release another's channel");
            action.finish(ExecutionSession.EndReason.COMPLETED);
            h.assertTrue(!ExecutionSessions.isOccupied(actor,ExecutionClaim.ENTITY_ACTION),"Action claim released independently");
            var position=session(h,frame.withExecutor(null),List.of(),starts,()->{});sessions.add(position);
            h.assertTrue(ExecutionSessions.start(position) && !ExecutionSessions.isCasting(root),"Fixed-point output does not claim the caster");
            for(int i=0;i<2;i++){
                var legacy=new ExecutionSession<>(h.getLevel(),CastContext.detached(root,null,null),0,30,new ExecutionDriver<Integer>(){
                    public boolean occupiesCaster(){return true;}
                    public boolean start(ExecutionSession<Integer> s){return true;}
                    public boolean tick(ExecutionSession<Integer> s){return false;}
                    public void close(ExecutionSession<Integer> s,ExecutionSession.EndReason reason){}
                },hit->{},()->true,ExecutionScope.DIRECT);
                sessions.add(legacy);h.assertTrue(ExecutionSessions.start(legacy),"Legacy activity flags retain concurrent-start behavior");
            }
            actor.discard();ExecutionSessions.tick();
            h.assertTrue(!two.active() && position.active(),"Executor loss cancels its channel but not a separate fixed-point output");h.succeed();
        }finally{for(var s:sessions)s.finish(ExecutionSession.EndReason.SERVER_STOPPED);root.discard();actor.discard();ExecutionSessions.tick();}
    }
    public static void reset(GameTestHelper h) {
        var root=cow(h,1);
        try {
            var ars=new SpellContext(h.getLevel(),new Spell(MethodSelf.INSTANCE),root,LivingCaster.from(root),ItemStack.EMPTY);
            TriggerGeometry.write(ars,new Vec3(1,0,0));var clone=ars.clone();TriggerGeometry.clear(clone);
            h.assertTrue(TriggerGeometry.read(clone).equals(Vec3.ZERO) && TriggerGeometry.read(ars).x>.99,"Explicit clear is isolated from sibling context");
            TriggerGeometry.scoped(new Vec3(0,0,1),()->TriggerGeometry.scoped(Vec3.ZERO,()->h.assertTrue(TriggerGeometry.read(ars).equals(Vec3.ZERO),"Zero scope masks outer geometry")));
            TriggerGeometry.write(ars,Vec3.ZERO);h.assertTrue(TriggerGeometry.read(ars).equals(Vec3.ZERO),"Explicit zero resets a stored direction");h.succeed();
        }finally{root.discard();}
    }

    public static void replacement(GameTestHelper h) {
        var root=cow(h,1);var actor=cow(h,3);var other=cow(h,5);
        var frame=CastContext.detached(root,null,null).withExecutor(actor);
        var owned=new ArrayList<ExecutionSession<Integer>>();var order=new ArrayList<String>();
        var funds=new AtomicInteger(100);var closes=new AtomicInteger();
        var account=new dev.arsconflux.api.resource.ResourceAccount(){
            public Object reservationKey(){return funds;}
            public boolean canSpend(int n){return funds.get()>=n;}
            public void spend(int n){funds.addAndGet(-n);}
        };
        try {
            var channel=session(h,frame,List.of(ExecutionClaim.channel(actor)),new AtomicInteger(),()->{});owned.add(channel);ExecutionSessions.start(channel);
            var legacy=new ExecutionSession<>(h.getLevel(),frame,0,30,new ExecutionDriver<Integer>(){
                public boolean occupiesCaster(){return true;}
                public boolean start(ExecutionSession<Integer> s){return true;}
                public boolean tick(ExecutionSession<Integer> s){return false;}
                public void close(ExecutionSession<Integer> s,ExecutionSession.EndReason reason){}
            },hit->{},()->true,ExecutionScope.DIRECT);owned.add(legacy);ExecutionSessions.start(legacy);
            var independent=session(h,frame.withExecutor(other),List.of(ExecutionClaim.action(other)),new AtomicInteger(),()->{});owned.add(independent);ExecutionSessions.start(independent);
            var nested=session(h,frame,List.of(ExecutionClaim.action(actor)),new AtomicInteger(),()->{});owned.add(nested);
            var old=new ExecutionSession<>(h.getLevel(),frame,0,30,new ExecutionDriver<Integer>(){
                public List<ExecutionClaim> claims(ExecutionSession<Integer> s){return List.of(ExecutionClaim.action(actor));}
                public boolean start(ExecutionSession<Integer> s){order.add("old-start");return true;}
                public boolean tick(ExecutionSession<Integer> s){return false;}
                public void close(ExecutionSession<Integer> s,ExecutionSession.EndReason reason){
                    closes.incrementAndGet();order.add("old-close");
                    if(reason==ExecutionSession.EndReason.DENIED)h.assertTrue(!ExecutionSessions.startReplacing(nested),"Cleanup cannot steal the incoming reservation");
                }
            },hit->{},()->true,ExecutionScope.DIRECT);owned.add(old);
            h.assertTrue(ExecutionSessions.start(old),"Legacy activity and channel can coexist with a physical action");
            var denied=new ExecutionSession<>(h.getLevel(),frame,0,30,new ExecutionDriver<Integer>(){
                public List<ExecutionClaim> claims(ExecutionSession<Integer> s){return List.of(ExecutionClaim.action(actor));}
                public boolean start(ExecutionSession<Integer> s){throw new AssertionError("Denied driver ran");}
                public boolean tick(ExecutionSession<Integer> s){return false;}
                public void close(ExecutionSession<Integer> s,ExecutionSession.EndReason reason){}
            },hit->{},()->false,ExecutionScope.DIRECT);owned.add(denied);
            h.assertTrue(!ExecutionSessions.startReplacing(denied) && old.active() && closes.get()==0,"Denied permission preserves the old execution");
            var empty=session(h,frame,List.of(ExecutionClaim.action(actor)),new AtomicInteger(),()->{throw new AssertionError("Unfunded driver ran");});
            empty.billing(TriggerPayment.of(new dev.arsconflux.api.resource.ResourceCharge(account,30)),false);owned.add(empty);funds.set(0);
            h.assertTrue(!ExecutionSessions.startReplacing(empty) && old.active() && closes.get()==0 && funds.get()==0,"Failed payment does not interrupt or spend");
            var next=session(h,frame,List.of(ExecutionClaim.action(actor)),new AtomicInteger(),()->{
                order.add("new-start");h.assertTrue(!old.active() && closes.get()==1,"Old cleanup finishes before the new driver starts");
            });next.billing(TriggerPayment.of(new dev.arsconflux.api.resource.ResourceCharge(account,30)),false);owned.add(next);funds.set(100);
            h.assertTrue(ExecutionSessions.startReplacing(next) && next.active() && !old.active() && funds.get()==70,"Replacement pays once");
            h.assertTrue(order.equals(List.of("old-start","old-close","new-start")) && channel.active() && legacy.active() && independent.active(),"Only the actual actor's conflicting claim is replaced");
            h.assertTrue(ExecutionSessions.isCasting(actor,c->c.channel().equals(ExecutionClaim.ACTIVITY)) && !ExecutionSessions.isCasting(root),"Filtered activity query distinguishes executor from payer");
            h.assertTrue(!ExecutionSessions.startReplacing(next) && next.active(),"Duplicate replacement cannot cancel itself");
            var position=session(h,frame.withExecutor(null),List.of(),new AtomicInteger(),()->{});owned.add(position);
            h.assertTrue(ExecutionSessions.startReplacing(position) && next.active(),"Fixed position does not interrupt an entity action");
            next.finish(ExecutionSession.EndReason.COMPLETED);
            h.assertTrue(!ExecutionSessions.isOccupied(actor,ExecutionClaim.ENTITY_ACTION) && channel.active() && legacy.active(),"Ending replacement releases only its own claim");
            h.succeed();
        }finally{
            for(var s:owned)s.finish(ExecutionSession.EndReason.SERVER_STOPPED);
            root.discard();actor.discard();other.discard();ExecutionSessions.tick();
        }
    }

}
