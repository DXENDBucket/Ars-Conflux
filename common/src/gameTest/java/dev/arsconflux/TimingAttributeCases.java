package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHeal;
import dev.arsconflux.api.casting.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class TimingAttributeCases {
    public static void cooldowns(GameTestHelper h, LivingEntity actor, AttributeInstance cooldown, AttributeInstance chant) {
        h.assertTrue(cooldown != null && chant != null, "Both synchronized attributes are attached to living entities without Iron/Goety");
        h.assertTrue(cooldown.getValue() == 0 && chant.getValue() == 0 && CastingTiming.cooldownTicks(actor, 80) == 80,
                "New attributes are neutral by default");
        var store = PersistentCooldowns.register(ResourceLocation.tryParse("timing_test:"+UUID.randomUUID()), "timing_"+UUID.randomUUID());
        cooldown.setBaseValue(.25);
        store.extend(actor, "direct", 80);
        h.assertTrue(store.remaining(actor, "direct") == 60, "Provider-native 80 ticks receive one final 25% reduction");
        var spell = new Spell(MethodSelf.INSTANCE);
        var context = SpellContext.fromEntity(spell, actor, ItemStack.EMPTY);
        ActiveCasts.execute(spell, actor, ActiveCastSource.CASTER_TOOL, () -> {
            store.commit(actor, "result", "success", 80);
            cooldown.setBaseValue(.5);
            ActiveCasts.dispatched(new SpellResolver(context), true);
            return true;
        }, false);
        h.assertTrue(store.remaining(actor, "result") == 60, "Deferred result snapshots reduction once and does not scale twice");
        h.assertTrue(store.remaining(actor, "direct") == 60, "Changing gear does not re-scale an existing deadline");
        store.extend(actor, "direct", 20);
        h.assertTrue(store.remaining(actor, "direct") == 60, "A shorter new source cannot shorten a running cooldown");
        store.extendRaw(actor, "raw", 80);
        h.assertTrue(store.remaining(actor, "raw") == 80, "Explicit final-duration writes remain available");
        cooldown.setBaseValue(1);
        store.extend(actor, "zero", 80);
        h.assertTrue(store.remaining(actor, "zero") == 0 && CastingTiming.cooldownTicks(actor,0) == 0,
                "100% is zero cooldown and zero-duration abilities stay zero");
        cooldown.setBaseValue(-.25);
        h.assertTrue(CastingTiming.cooldownTicks(actor,80) == 100, "Negative modifiers can lengthen cooldowns");
        h.assertTrue(CastingTiming.scale(Long.MAX_VALUE,0,Long.MAX_VALUE)==Long.MAX_VALUE
                && CastingTiming.scale(Long.MAX_VALUE,-10,Long.MAX_VALUE)==Long.MAX_VALUE,
                "Large durations preserve or saturate instead of overflowing");
        actor.discard(); h.succeed();
    }
    public static void preparation(GameTestHelper h, LivingEntity actor, AttributeInstance cooldown, AttributeInstance chant) {
        actor.setNoGravity(true);
        actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        chant.setBaseValue(.5);
        var recipe=new Spell(MethodSelf.INSTANCE,EffectHeal.INSTANCE);
        var starts=new AtomicInteger();var releases=new AtomicInteger();
        int combined=ChantDurations.combine(List.of(4,8),dev.arsconflux.config.ConfluxConfig.sumChants());
        int expected=combined/2;
        for(int duration:List.of(4,8)) ActiveCasts.register(ResourceLocation.tryParse("timing_test:p"+UUID.randomUUID()),new ActiveCastProvider(){
            public boolean supports(Spell spell){return spell==recipe;}
            public boolean chantingEnabled(ActiveCastRequest request){return true;}
            public List<Integer> chantTicks(ActiveCastRequest request){return List.of(duration);}
            public void chantProgress(ActiveCastRequest request,UUID token,int total,int remaining){
                if(remaining==total){h.assertTrue(total==expected,"Each provider sees the same once-reduced MAX/SUM preparation");starts.incrementAndGet();}
            }
        });
        var deferred=ActiveCasts.defer(recipe,actor,InteractionHand.MAIN_HAND,ActiveCastSource.CASTER_TOOL,releases::incrementAndGet);
        h.assertTrue(deferred==ActiveCasts.Deferral.QUEUED&&starts.get()==2,"Mixed providers share one reduced preparation");
        chant.setBaseValue(1); // Changing gear midway must not resize this preparation.
        h.runAfterDelay(2,()->h.assertTrue(ActiveCasts.isChanting(actor)&&releases.get()==0,"Pending preparation retains its initial duration"));
        h.runAfterDelay(expected+2,()->{
            h.assertTrue(!ActiveCasts.isChanting(actor)&&releases.get()==1,"Prepared action releases exactly once");
            h.assertTrue(ActiveCasts.defer(recipe,actor,InteractionHand.MAIN_HAND,ActiveCastSource.CASTER_TOOL,()->{})==ActiveCasts.Deferral.UNNEEDED,
                    "100% preparation reduction releases without a zero-length queue");
            var pure=new Spell(MethodSelf.INSTANCE,EffectHeal.INSTANCE);
            h.assertTrue(ActiveCasts.defer(pure,actor,InteractionHand.MAIN_HAND,ActiveCastSource.CASTER_TOOL,()->{})==ActiveCasts.Deferral.UNNEEDED,
                    "Unclaimed ordinary Ars spells do not gain a chant");
            actor.discard();h.succeed();
        });
    }
}
