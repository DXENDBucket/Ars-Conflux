package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.effect.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.damage.NativeSpellDamage;
import dev.arsconflux.api.school.*;
import dev.arsconflux.config.ConfluxConfig;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import java.util.Map;

public final class SchoolTestCases {
    private static final net.minecraft.resources.ResourceLocation SUN = net.minecraft.resources.ResourceLocation.tryParse("ars_conflux:sun");
    private static final net.minecraft.resources.ResourceLocation DEATH = net.minecraft.resources.ResourceLocation.tryParse("ars_conflux:death");

    private static LivingEntity victim(GameTestHelper h,int x) {
        var entity=h.spawn(EntityType.COW,x,3,2);entity.setNoAi(true);entity.setNoGravity(true);
        entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);entity.setHealth(500);return entity;
    }
    private static void castHarm(GameTestHelper h,LivingEntity caster,LivingEntity target) {
        var context=SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE,EffectHarm.INSTANCE),caster,ItemStack.EMPTY);
        context.setCurrentIndex(1);new SpellResolver(context).onResolveEffect(h.getLevel(),new EntityHitResult(target));
    }
    public static void run(GameTestHelper h) {
        boolean old=ConfluxConfig.SCHOOL_DAMAGE_RULES.get(),billing=ConfluxConfig.VANILLA_TRIGGER_BILLING.get();
        var caster=DamageTestAccess.caster(h.getLevel());var target=victim(h,2);var other=victim(h,4);
        var frame=CastContext.detached(caster,new EntityHitResult(target),null);
        var weights=SchoolWeights.of(Map.of(SUN,7,DEATH,3));
        var source=h.getLevel().damageSources().magic();
        var payload=h.spawn(EntityType.ARMOR_STAND,1,4,1);payload.setNoGravity(true);
        try {
            ConfluxConfig.VANILLA_TRIGGER_BILLING.set(false);
            h.assertTrue(GlyphSchools.get(EffectHarm.INSTANCE).equals(weights),"Actual server datapack declares Harm's weighted profile");
            h.assertTrue(GlyphSchools.get(EffectIgnite.INSTANCE).isEmpty(),"Original Ars school membership does not infer a Conflux declaration");
            boolean nativeMember=SpellSchools.ELEMENTAL_FIRE.isPartOfSchool(EffectHarm.INSTANCE);
            SchoolRegistry.associate(SUN,SpellSchools.ELEMENTAL_FIRE);
            h.assertTrue(SchoolRegistry.nativeSchoolIds(SUN).contains(SpellSchools.ELEMENTAL_FIRE.getId())&&nativeMember==SpellSchools.ELEMENTAL_FIRE.isPartOfSchool(EffectHarm.INSTANCE),"Native associations retain original Ars membership");
            ConfluxConfig.SCHOOL_DAMAGE_RULES.set(false);
            NativeSpellDamage.hurt(frame,weights,target,source,10,(s,n)->{h.assertTrue(s==source&&n==16,"Disabled rules retain native source and existing Ars bonus");return true;});
            castHarm(h,caster,other);float baseline=500-other.getHealth();h.assertTrue(baseline>0,"Real native Harm baseline actually damages");
            ConfluxConfig.SCHOOL_DAMAGE_RULES.set(true);
            NativeSpellDamage.hurt(frame,weights,target,source,10,(s,n)->{h.assertTrue(s==source&&Math.abs(n-29.6)<0.001,"Weighted school rules apply once after the common flat bonus");return true;});
            SchoolDamage.during(weights,()->NativeSpellDamage.hurt(frame,SchoolWeights.EMPTY,target,source,10,(s,n)->{h.assertTrue(n==16,"Explicit undeclared hit masks outer school scope");return true;}));
            NativeSpellDamage.hurt(frame,target,source,10,(s,n)->{h.assertTrue(n==16,"Undeclared existing API callers remain unchanged");return true;});
            NativeSpellDamage.during(frame,weights,()->target.hurt(source,10));
            h.assertTrue(Math.abs(500-target.getHealth()-29.6)<0.001,"Scoped raw native damage and its hurt hook do not double-apply schools");
            var classified=victim(h,6);castHarm(h,caster,classified);
            h.assertTrue(Math.abs((500-classified.getHealth())-baseline*1.85)<0.001,"Ordinary Ars effect dispatch uses its own declared school profile");
            NativeSpellDamage.bind(payload,frame,weights);
            var indirect=h.getLevel().damageSources().indirectMagic(payload,caster);
            NativeSpellDamage.hurt(frame,SchoolWeights.EMPTY,target,indirect,10,(s,n)->{h.assertTrue(n==16,"Explicit per-hit empty profile can override a classified payload");return true;});
            h.runAfterDelay(3,()->{
                try {
                    NativeSpellDamage.hurt(NativeSpellDamage.context(payload),target,indirect,10,(s,n)->{h.assertTrue(Math.abs(n-29.6)<0.001,"Delayed payload preserves immutable school profile outside dispatch scope: "+n);return true;});
                    h.assertTrue(SchoolDamage.current().isEmpty(),"School scope has no leak after delayed hit");
                    NativeSpellDamage.bind(payload,null);h.assertTrue(SchoolDamage.profile(payload).isEmpty(),"Unbinding releases school attachment");h.succeed();
                } finally {ConfluxConfig.SCHOOL_DAMAGE_RULES.set(old);ConfluxConfig.VANILLA_TRIGGER_BILLING.set(billing);payload.discard();}
            });
        } catch(RuntimeException|Error failure) {
            ConfluxConfig.SCHOOL_DAMAGE_RULES.set(old);ConfluxConfig.VANILLA_TRIGGER_BILLING.set(billing);NativeSpellDamage.bind(payload,null);payload.discard();throw failure;
        }
    }
    private SchoolTestCases() {}
}
