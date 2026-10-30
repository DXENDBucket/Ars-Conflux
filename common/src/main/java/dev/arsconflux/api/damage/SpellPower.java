package dev.arsconflux.api.damage;

import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.school.SchoolDamage;
import dev.arsconflux.api.school.SchoolWeights;
import dev.arsconflux.config.ConfluxConfig;
import dev.arsconflux.platform.PowerAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import java.util.*;
import java.util.function.Supplier;

/** Percentage damage bonuses. Native spell parameters, healing, costs and attribution are untouched. */
public final class SpellPower {
    public record Profile(LivingEntity caster, SchoolWeights schools, LivingEntity executor, LivingEntity damageOwner) {
        public Profile { Objects.requireNonNull(schools); }
        public Profile(LivingEntity caster,SchoolWeights schools) {this(caster,schools,caster,caster);}
        public Profile(CastContext context,SchoolWeights schools) {this(context.caster(),schools,context.executor(),context.damageOwner());}
        public Profile withSchools(SchoolWeights next) {return new Profile(caster,next,executor,damageOwner);}
        boolean owns(DamageSource source) {
            var owner=source==null?null:source.getEntity();
            return owner==null||owner==caster||owner==executor||owner==damageOwner;
        }
    }
    private static final Map<Entity, Profile> PAYLOADS = new WeakHashMap<>();
    private static final ThreadLocal<Profile> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> HANDLED = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ARS_EVENT = new ThreadLocal<>();

    public static ResourceLocation attributeId(ResourceLocation school) {
        return ResourceLocation.tryParse("ars_conflux:school_power/"+school.getNamespace()+"/"+school.getPath());
    }
    public static double multiplier(LivingEntity caster, SchoolWeights schools) {
        double bonus=PowerAttributes.general(caster);
        for(var entry:schools.shares().entrySet())bonus+=entry.getValue()*PowerAttributes.school(caster,entry.getKey());
        return Math.max(0,1+bonus);
    }
    public static Profile current() { return CURRENT.get(); }
    public static Profile capture(LivingEntity caster) {
        var context=dev.arsconflux.api.context.CastContexts.current();
        return context!=null&&context.caster()==caster?new Profile(context,SchoolDamage.current()):new Profile(caster,SchoolDamage.current());
    }
    public static void bind(Entity entity, Profile profile) {
        Objects.requireNonNull(entity);
        if(profile==null)PAYLOADS.remove(entity);else PAYLOADS.put(entity,profile);
    }
    public static Profile profile(Entity entity) { return PAYLOADS.get(entity); }
    /** Explicit opt-in for non-Ars damage paths; restores the previous context on exceptions. */
    public static <T> T during(CastContext context, SchoolWeights schools, Supplier<T> action) {
        return scoped(context==null?null:new Profile(context,schools),action);
    }
    public static <T> T scoped(Profile profile, Supplier<T> action) {
        var old=CURRENT.get();if(profile==null)CURRENT.remove();else CURRENT.set(profile);
        try{return action.get();}finally{if(old==null)CURRENT.remove();else CURRENT.set(old);}
    }
    private static Profile nativeProfile(DamageSource source) {
        var direct=source==null?null:PAYLOADS.get(source.getDirectEntity());
        var profile=CURRENT.get();
        if(profile!=null&&(profile.owns(source)||direct!=null&&direct.caster()==profile.caster()))
            return SchoolDamage.hasScope()?profile.withSchools(SchoolDamage.current()):profile;
        return direct;
    }
    /** Shared Ars Pre event versus final native hurt are mutually exclusive scaling boundaries. */
    public static boolean useArsEvent(DamageSource source) {
        return !Boolean.TRUE.equals(HANDLED.get())&&(Boolean.TRUE.equals(ARS_EVENT.get())||nativeProfile(source)==null);
    }
    public static <T> T arsEvent(Supplier<T> action) {return flag(ARS_EVENT,action);}
    public static <T> T handled(Supplier<T> action) {return flag(HANDLED,action);}
    private static <T> T flag(ThreadLocal<Boolean> key,Supplier<T> action) {
        var old=key.get();key.set(true);
        try{return action.get();}finally{if(old==null)key.remove();else key.set(old);}
    }
    public static boolean hurt(Entity target,DamageSource source,float amount,NativeSpellDamage.Hit original) {
        if(Boolean.TRUE.equals(HANDLED.get())||target.level().isClientSide||!Float.isFinite(amount)||amount<=0)return original.hurt(source,amount);
        var profile=nativeProfile(source);
        if(profile==null||profile.caster()==null||!profile.caster().isAlive())return original.hurt(source,amount);
        double factor=multiplier(profile.caster(),profile.schools());
        if(factor<=0)return false;
        if(ConfluxConfig.schoolDamageRules())factor*=SchoolDamage.multiplier(profile.schools(),target);
        float adjusted=(float)Math.min(Float.MAX_VALUE,amount*factor);
        if(adjusted<=0)return false;
        return handled(()->original.hurt(source,adjusted));
    }
    /** Capture only spell-created nonliving payloads, never ordinary summoned creatures' melee. */
    public static void spawned(Entity entity) {
        var profile=CURRENT.get();
        if(profile!=null&&!(entity instanceof LivingEntity)&&!PAYLOADS.containsKey(entity))
            bind(entity,profile.withSchools(SchoolDamage.hasScope()?SchoolDamage.current():profile.schools()));
    }
    /** Native emitters retain identity for damage and child payloads during their own tick. */
    public static void tick(Entity entity,Runnable action) {
        var profile=PAYLOADS.get(entity);
        if(profile==null){action.run();return;}
        SchoolDamage.during(profile.schools(),()->scoped(profile,()->{action.run();return null;}));
    }
    public static void unload(Level world){PAYLOADS.keySet().removeIf(entity->entity.level()==world);}
    public static void stop(){PAYLOADS.clear();CURRENT.remove();HANDLED.remove();ARS_EVENT.remove();}
    private SpellPower() {}
}
