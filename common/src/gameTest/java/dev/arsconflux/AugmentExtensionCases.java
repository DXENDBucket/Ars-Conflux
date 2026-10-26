package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.resource.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

public final class AugmentExtensionCases {
    static AbstractSpellPart part(String simple, String prefix) {
        return GlyphRegistry.getSpellpartMap().values().stream().filter(p -> p.getClass().getSimpleName().equals(simple)
                && p.getClass().getName().contains(prefix)).findFirst().orElse(null);
    }
    static AbstractSpellPart id(String id) { return GlyphRegistry.getSpellPart(dev.arsconflux.api.projectile.CarrierRegistry.id(id)); }
    static LivingEntity cow(GameTestHelper h, int x) {
        var cow=h.spawn(EntityType.COW,x,3,2);cow.setNoAi(true);cow.setNoGravity(true);return cow;
    }
    static final class Account implements ResourceAccount {
        int balance=100000;
        public Object reservationKey(){return this;}
        public boolean canSpend(int amount){return balance>=amount;}
        public void spend(int amount){balance-=amount;}
    }
    static SpellResolver resolver(GameTestHelper h, LivingEntity caster, AbstractSpellPart effect,
                                  Account account, AbstractAugment... augments) {
        var parts=new ArrayList<AbstractSpellPart>();parts.add(MethodTouch.INSTANCE);parts.add(effect);parts.addAll(List.of(augments));
        var source=new LivingCaster(caster) {
            public boolean enoughMana(int amount){return account.balance>=amount;}
            public void expendMana(int amount){account.balance-=amount;}
        };
        var context=new SpellContext(h.getLevel(),new Spell(parts),caster,source,ItemStack.EMPTY);
        CastContexts.bind(context,CastContext.of(context,null,account));
        return new SpellResolver(context);
    }
    static SpellStats stats(GameTestHelper h,LivingEntity caster,AbstractSpellPart effect,SpellResolver resolver,HitResult hit,AbstractAugment... augments) {
        return new SpellStats.Builder().setAugments(new ArrayList<>(List.of(augments))).build(effect,hit,h.getLevel(),caster,resolver.spellContext);
    }
    public static void blocks(GameTestHelper h) {
        var caster=cow(h,2);
        var flatten=(AbstractEffect)Objects.requireNonNull(part("EffectFlatten","not_enough_glyphs"));
        var annihilate=(AbstractEffect)Objects.requireNonNull(part("EffectAnnihilate","adamsarsplus"));
        h.assertTrue(flatten.compatibleAugments.containsAll(Set.of(AugmentAOE.INSTANCE,AugmentPierce.INSTANCE))
                && annihilate.compatibleAugments.containsAll(Set.of(AugmentAOE.INSTANCE,AugmentPierce.INSTANCE,AugmentSensitive.INSTANCE)),"Missing native augment families are now offered");
        for(int pass=0;pass<2;pass++) {
            var effect=pass==0?flatten:annihilate;
            var center=h.absolutePos(new BlockPos(4+pass*7,3,5));
            var hit=new BlockHitResult(Vec3.atCenterOf(center).add(0,.5,0),Direction.UP,center,false);
            var positions=com.hollingsworth.arsnouveau.api.util.SpellUtil.calcAOEBlocks(caster,center,hit,1,0);
            for(var pos:positions) {
                h.getLevel().setBlockAndUpdate(pos,Blocks.DIRT.defaultBlockState());
                h.getLevel().setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
            }
            var resolver=resolver(h,caster,effect,new Account(),AugmentAOE.INSTANCE,AugmentAmplify.INSTANCE);
            effect.onResolveBlock(hit,h.getLevel(),caster,stats(h,caster,effect,resolver,hit,AugmentAOE.INSTANCE,AugmentAmplify.INSTANCE),resolver.spellContext,resolver);
            int changed=0;
            for(var pos:positions)if(h.getLevel().getBlockState(pos).is(pass==0?Blocks.DIRT_PATH:Blocks.AIR))changed++;
            h.assertTrue(changed==positions.size()&&changed>1,"Every native AOE block uses its own target; "+effect.getClass().getSimpleName()+" changed "+changed+" of "+positions.size());
        }
        h.succeed();
    }
    public static void shock(GameTestHelper h) {
        var caster=cow(h,2);var target=cow(h,5);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
        var smite=(AbstractEffect)Objects.requireNonNull(part("EffectDivineSmite","adamsarsplus"));
        h.assertTrue(smite.compatibleAugments.containsAll(Set.of(AugmentExtendTime.INSTANCE,AugmentDurationDown.INSTANCE)),"Smite offers both duration augments");
        var effectId=BuiltInRegistries.MOB_EFFECT.keySet().stream().filter(k->k.getNamespace().equals("ars_nouveau")&&k.getPath().contains("shocked")).findFirst().orElseThrow();
        Object holder=BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.MOB_EFFECT,effectId)).orElseThrow();
        Object effect=BuiltInRegistries.MOB_EFFECT.get(effectId);
        try {
            var ctor=Arrays.stream(MobEffectInstance.class.getConstructors()).filter(c->c.getParameterCount()==3&&c.getParameterTypes()[1]==int.class).findFirst().orElseThrow();
            Object reference=ctor.getParameterTypes()[0].isInstance(holder)?holder:effect;
            target.addEffect((MobEffectInstance)ctor.newInstance(reference,1200,2));
        } catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
        var hit=new EntityHitResult(target);var resolver=resolver(h,caster,smite,new Account(),AugmentDurationDown.INSTANCE);
        smite.onResolve(hit,h.getLevel(),caster,stats(h,caster,smite,resolver,hit,AugmentDurationDown.INSTANCE),resolver.spellContext,resolver);
        var bolts=h.getLevel().getEntities(caster,caster.getBoundingBox().inflate(12),e->e.getClass().getSimpleName().equals("EntityDivineSmite"));
        h.assertTrue(bolts.size()==1,"Native divine lightning is spawned");
        bolts.get(0).tick();
        var shocked=target.getActiveEffects().stream().filter(e->e.getEffect().equals(holder)||e.getEffect().equals(effect)).findFirst().orElse(null);
        h.assertTrue(shocked!=null&&shocked.getDuration()==1,"Negative shock duration saturates at one tick");
        bolts.forEach(Entity::discard);h.succeed();
    }
    public static void precise(GameTestHelper h) {
        var delay=(AbstractEffect)part("EffectPreciseDelay","ars_controle");
        if(delay==null){h.succeed();return;} // Upstream Forge has no such glyph.
        var extend=(AbstractAugment)Objects.requireNonNull(part("AugmentExtendTimeThree","adamsarsplus"));
        var caster=cow(h,2);var hit=new EntityHitResult(caster);
        var resolver=resolver(h,caster,delay,new Account(),extend);
        resolver.spellContext.nextPart();resolver.spellContext.nextPart();
        delay.onResolveEntity(hit,h.getLevel(),caster,stats(h,caster,delay,resolver,hit,extend),resolver.spellContext,resolver);
        h.assertTrue(resolver.spellContext.getDelayedSpellEvent()!=null&&resolver.spellContext.getDelayedSpellEvent().duration==16,"One AAP tier-III Extend Time counts as four native augments: 2^4 ticks");
        h.succeed();
    }
}
