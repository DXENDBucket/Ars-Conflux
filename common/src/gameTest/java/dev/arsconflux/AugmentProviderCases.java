package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.EntityHitResult;
import java.util.*;

/** Production provider JARs and native entities, without provider compile dependencies. */
public final class AugmentProviderCases {
    @SuppressWarnings("unchecked") private static List<ExecutionSession<?>> sessions(LivingEntity caster) {
        try {
            var field=ExecutionSessions.class.getDeclaredField("ACTIVE");field.setAccessible(true);
            return ((Map<UUID,ExecutionSession<?>>)field.get(null)).values().stream().filter(s->s.caster()==caster&&s.active()).toList();
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static AbstractEffect effect(String id) {return (AbstractEffect)Objects.requireNonNull(AugmentExtensionCases.id(id),id);}
    private static void cast(GameTestHelper h,LivingEntity caster,LivingEntity target,AbstractEffect effect,
                             AugmentExtensionCases.Account account,AbstractAugment... augments) {
        var resolver=AugmentExtensionCases.resolver(h,caster,effect,account,augments);var hit=new EntityHitResult(target);
        effect.onResolve(hit,h.getLevel(),caster,AugmentExtensionCases.stats(h,caster,effect,resolver,hit,augments),resolver.spellContext,resolver);
    }
    public static void growth(GameTestHelper h) throws Exception {
        var mowzie=AugmentExtensionCases.cow(h,4);mowzie.setInvulnerable(true);
        var beam=effect("mowzies_nouveau:glyph_solar_beam");
        h.assertTrue(beam.compatibleAugments.containsAll(Set.of(AugmentDampen.INSTANCE,AugmentDurationDown.INSTANCE)),"Mowzie offers numeric reductions");
        cast(h,mowzie,mowzie,beam,new AugmentExtensionCases.Account(),AugmentDampen.INSTANCE,AugmentDurationDown.INSTANCE);
        var nativeBeam=h.getLevel().getEntities(mowzie,mowzie.getBoundingBox().inflate(8),e->e.getClass().getSimpleName().equals("SpellSolarBeam")).stream().findFirst().orElseThrow();
        h.assertTrue(((Number)nativeBeam.getClass().getMethod("amplification").invoke(nativeBeam)).doubleValue()==-1,"Signed amplification reaches the native Mowzie effect");
        int extend=((Number)Class.forName("dev.mowziesnouveau.platform.MowzieConfig").getMethod("extendTicks").invoke(null)).intValue();
        int mowzieBase=Class.forName("dev.mowziesnouveau.Ability").getField("activeTicks").getInt(Class.forName("dev.mowziesnouveau.Ability").getField("SOLAR_BEAM").get(null));
        int actualDuration=((Number)nativeBeam.getClass().getMethod("getDuration").invoke(nativeBeam)).intValue();
        h.assertTrue(actualDuration==Math.max(1,mowzieBase-extend),"Mowzie channel: "+actualDuration+" vs "+mowzieBase+" - "+extend);
        var victim=AugmentExtensionCases.cow(h,6);float health=victim.getHealth();
        var hurt=Class.forName("dev.mowziesnouveau.NativeDamage").getMethod("hurt",Entity.class,Entity.class,net.minecraft.world.damagesource.DamageSource.class,float.class);
        h.assertTrue((boolean)hurt.invoke(null,nativeBeam,victim,h.getLevel().damageSources().magic(),10f)
                && health-victim.getHealth()>0&&health-victim.getHealth()<10,"Mowzie Dampen reduces actual damage below its native base");
        sessions(mowzie).forEach(s->s.finish(ExecutionSession.EndReason.EXPIRED));

        var aqua=AugmentExtensionCases.cow(h,8);aqua.setInvulnerable(true);var target=AugmentExtensionCases.cow(h,9);
        var sting=effect("aquamirae_nouveau:glyph_scyphoid_sting");
        cast(h,aqua,target,sting,new AugmentExtensionCases.Account(),AugmentDampen.INSTANCE,AugmentDurationDown.INSTANCE);
        int aquaExtend=((Number)Class.forName("dev.aquamiraenouveau.platform.AquamiraeConfig").getMethod("extendTicks").invoke(null)).intValue();
        int baseTicks=((Number)Class.forName("dev.aquamiraenouveau.platform.AquamiraeConfig").getMethod("duration",Class.forName("dev.aquamiraenouveau.Ability"))
                .invoke(null,Class.forName("dev.aquamiraenouveau.Ability").getField("SCYPHOID_STING").get(null))).intValue();
        h.assertTrue(target.getActiveEffects().stream().anyMatch(e->e.getAmplifier()==1&&e.getDuration()==Math.max(1,baseTicks-aquaExtend)),"Aquamirae sting has lower native poison level and shorter duration");

        var cat=AugmentExtensionCases.cow(h,12);cat.setInvulnerable(true);
        h.getLevel().setBlockAndUpdate(cat.blockPosition().below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        for(String id:List.of("wither_smoke","lightning_field")) {
            var effect=effect("cataclysm_nouveau:glyph_"+id);
            h.assertTrue(effect.compatibleAugments.contains(AugmentAOE.INSTANCE),"Cataclysm area augment is offered");
            cast(h,cat,cat,effect,new AugmentExtensionCases.Account(),AugmentAOE.INSTANCE);
        }
        var clouds=h.getLevel().getEntities(cat,cat.getBoundingBox().inflate(10),e->Set.of("Wither_Smoke_Effect_Entity","Lightning_Area_Effect_Entity").contains(e.getClass().getSimpleName()));
        h.assertTrue(clouds.size()==2,"Both native Cataclysm area entities exist: "+clouds.stream().map(e->e.getClass().getSimpleName()).toList());
        for(var cloud:clouds) {
            h.assertTrue(Math.abs(((Number)cloud.getClass().getMethod("getRadius").invoke(cloud)).doubleValue()-4)<.001,"AOE enlarges the native synced radius to four");
            float decay=((Number)cloud.getClass().getMethod("getRadiusPerTick").invoke(cloud)).floatValue();
            int duration=((Number)cloud.getClass().getMethod("getDuration").invoke(cloud)).intValue();
            float end=cloud.getClass().getSimpleName().startsWith("Wither")?0:.5f;
            h.assertTrue(Math.abs(4+decay*duration-end)<.001,"Radius decay preserves the original final footprint");
            cloud.discard();
        }

        var laser=effect("bomd_nouveau:glyph_gauntlet_laser");
        h.assertTrue(laser.compatibleAugments.containsAll(Set.of(AugmentExtendTime.INSTANCE,AugmentDurationDown.INSTANCE)),"BOMD laser offers duration augments");
        var longCaster=AugmentExtensionCases.cow(h,2);longCaster.setInvulnerable(true);
        var shortCaster=AugmentExtensionCases.cow(h,3);shortCaster.setInvulnerable(true);
        var longAccount=new AugmentExtensionCases.Account();var shortAccount=new AugmentExtensionCases.Account();
        var ctx=AugmentExtensionCases.resolver(h,longCaster,laser,longAccount,AugmentExtendTime.INSTANCE).spellContext;
        int fee=((TriggerPaidGlyph)laser).firstCharge(ctx,List.of(AugmentExtendTime.INSTANCE)).amount();
        shortAccount.balance=fee;
        cast(h,longCaster,longCaster,laser,longAccount,AugmentExtendTime.INSTANCE);
        cast(h,shortCaster,shortCaster,laser,shortAccount,AugmentExtendTime.INSTANCE);
        h.assertTrue(sessions(longCaster).size()==1&&sessions(longCaster).get(0).remainingTicks()==105,"BOMD session has 25 charge ticks plus 80 active ticks");
        h.runAfterDelay(50,()->h.assertTrue(sessions(shortCaster).isEmpty()&&shortAccount.balance==0,"An extended laser still stops on resource exhaustion"));
        h.runAfterDelay(90,()->h.assertTrue(!sessions(longCaster).isEmpty()&&longAccount.balance==100000-4*fee,"Extended output survives the old expiry and pays its additional active seconds"));
        h.runAfterDelay(110,()->{h.assertTrue(sessions(longCaster).isEmpty()&&longAccount.balance==100000-4*fee,"Laser closes at its new expiry without a fifth charge");h.succeed();});
    }
}
