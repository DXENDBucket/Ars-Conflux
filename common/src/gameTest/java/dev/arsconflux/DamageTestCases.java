package dev.arsconflux;

import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.damage.NativeSpellDamage;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.EntityHitResult;

public final class DamageTestCases {
    private static LivingEntity victim(GameTestHelper h) {
        var cow=h.spawn(EntityType.COW,2,3,2);cow.setNoAi(true);cow.setNoGravity(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);cow.setHealth(500);return cow;
    }
    public static void events(GameTestHelper h) {
        var caster=DamageTestAccess.caster(h.getLevel());var target=victim(h);
        var frame=CastContext.detached(caster,new EntityHitResult(target),null);
        var source=h.getLevel().damageSources().magic();var changed=h.getLevel().damageSources().fall();
        int[] calls={0,0};float[] quote={0};boolean[] cancel={false};
        try(var observer=DamageTestAccess.observe(e->{if(e.caster==caster && e.target==target){
            calls[0]++;quote[0]=e.damage;e.damage*=1.5F;e.damageSource=changed;if(cancel[0])e.setCanceled(true);
        }},e->{if(e.caster==caster && e.target==target)calls[1]++;})) {
            boolean success=NativeSpellDamage.hurt(frame,target,source,10,(s,n)->{
                h.assertTrue(s==changed && n==24,"Pre modifies the native source and damage after one flat bonus");return true;
            });
            h.assertTrue(success && quote[0]==16 && calls[0]==1 && calls[1]==1,"Single Pre and successful Post");
            cancel[0]=true;
            h.assertTrue(!NativeSpellDamage.hurt(frame,target,source,10,(s,n)->{throw new AssertionError("Cancelled hit ran");}),"Cancellation rejects hit");
            cancel[0]=false;
            h.assertTrue(!NativeSpellDamage.hurt(frame,target,source,10,(s,n)->false),"Native refusal is preserved");
            h.assertTrue(calls[0]==3 && calls[1]==1,"Neither cancellation nor native refusal emits Post");
            NativeSpellDamage.hurt(null,target,source,10,(s,n)->{h.assertTrue(s==source && n==10,"Unbound hit remains native");return true;});
            h.assertTrue(calls[0]==3,"No event for ordinary native attack");
            try {NativeSpellDamage.hurt(frame,target,source,10,(s,n)->{throw new IllegalStateException("expected");});}
            catch(IllegalStateException expected) {h.assertTrue(expected.getMessage().equals("expected"),"Expected callback failure");}
            NativeSpellDamage.hurt(frame,target,source,10,(s,n)->true);
            h.assertTrue(calls[0]==5 && calls[1]==2,"Exception releases dispatch guard");
            h.succeed();
        } catch(Exception e){throw new RuntimeException(e);}
    }
    public static void scopes(GameTestHelper h) {
        var caster=DamageTestAccess.caster(h.getLevel());var target=victim(h);
        var frame=CastContext.detached(caster,new EntityHitResult(target),null);
        var nested=frame.withExecutor(target);var source=h.getLevel().damageSources().magic();int[] count={0};
        try(var observer=DamageTestAccess.observe(e->{if(e.caster==caster)count[0]++;},e->{})) {
            NativeSpellDamage.during(frame,()->NativeSpellDamage.hurt(frame,target,source,10,target::hurt));
            h.assertTrue(500-target.getHealth()==16 && count[0]==1,"Provider hook plus shared LivingEntity hook applies one bonus/event");
            target.invulnerableTime=0;
            NativeSpellDamage.during(frame,()->target.hurt(source,10));
            h.assertTrue(500-target.getHealth()==32 && count[0]==2,"Native synchronous hit opts in through scope alone");
            NativeSpellDamage.during(frame,()->{
                try {NativeSpellDamage.during(nested,()->{h.assertTrue(NativeSpellDamage.currentScope()==nested,"Nested scope active");throw new IllegalStateException("expected");});}
                catch(IllegalStateException expected) {}
                h.assertTrue(NativeSpellDamage.currentScope()==frame,"Exception restores outer scope");
                NativeSpellDamage.during(null,()->{h.assertTrue(NativeSpellDamage.currentScope()==null,"Null masks outer scope");return null;});
                h.assertTrue(NativeSpellDamage.currentScope()==frame,"Masked scope restores outer scope");return null;
            });
            h.assertTrue(NativeSpellDamage.currentScope()==null,"No lingering scope");
            target.invulnerableTime=0;target.hurt(source,10);
            h.assertTrue(500-target.getHealth()==42 && count[0]==2,"Later unrelated hurt unchanged");h.succeed();
        } catch(Exception e){throw new RuntimeException(e);}
    }
    public static void attachments(GameTestHelper h) {
        var caster=DamageTestAccess.caster(h.getLevel());var parent=victim(h);var child=victim(h);
        var frame=CastContext.detached(caster,new EntityHitResult(child),null).withDamageOwner(parent);
        h.assertTrue(NativeSpellDamage.bind(parent,frame)==parent && NativeSpellDamage.context(parent)==frame,"Attachment preserves original roles");
        NativeSpellDamage.bind(child,NativeSpellDamage.context(parent));NativeSpellDamage.bind(parent,null);
        h.assertTrue(NativeSpellDamage.context(parent)==null,"Null unbinds");
        h.runAfterDelay(5,()->{
            NativeSpellDamage.hurt(NativeSpellDamage.context(child),child,h.getLevel().damageSources().magic(),10,child::hurt);
            h.assertTrue(500-child.getHealth()==16,"Delayed child uses its attached root caster's bonus");
            h.assertTrue(NativeSpellDamage.context(child).damageOwner()==parent,"Attachment preserves distinct damage owner");
            NativeSpellDamage.unload(h.getLevel());h.assertTrue(NativeSpellDamage.context(child)==null,"Unload clears world's attachments");h.succeed();
        });
    }
    private DamageTestCases() {}
}
