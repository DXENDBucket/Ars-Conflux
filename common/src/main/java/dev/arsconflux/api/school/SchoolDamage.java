package dev.arsconflux.api.school;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import dev.arsconflux.config.ConfluxConfig;
import dev.arsconflux.internal.school.SchoolData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import java.util.*;
import java.util.function.Supplier;

/** Per-effect/per-payload school identity, deliberately separate from a whole CastContext. */
public final class SchoolDamage {
    private static final Map<Entity,SchoolWeights> PAYLOADS=new WeakHashMap<>();
    private static final ThreadLocal<SchoolWeights> SCOPE=new ThreadLocal<>();
    public static boolean hasScope() {return SCOPE.get()!=null;}
    public static SchoolWeights current() { var weights=SCOPE.get();return weights==null?SchoolWeights.EMPTY:weights; }
    public static <T> T during(SchoolWeights weights,Supplier<T> action) {
        Objects.requireNonNull(weights);var previous=SCOPE.get();SCOPE.set(weights);
        try {return action.get();} finally {if(previous==null)SCOPE.remove();else SCOPE.set(previous);}
    }
    /** Snapshot for delayed payloads. An explicit empty profile masks an outer effect's schools. Server-thread only. */
    public static void bind(Entity entity,SchoolWeights weights) { PAYLOADS.put(Objects.requireNonNull(entity),Objects.requireNonNull(weights)); }
    public static void unbind(Entity entity) { PAYLOADS.remove(entity); }
    public static SchoolWeights profile(Entity entity) { return PAYLOADS.getOrDefault(entity,SchoolWeights.EMPTY); }
    public static SchoolWeights resolve(DamageSource source) {
        var direct=source==null?null:source.getDirectEntity();
        return direct!=null&&PAYLOADS.containsKey(direct)?PAYLOADS.get(direct):current();
    }
    public static double multiplier(SchoolWeights weights,Entity target) {
        return weights.multiplier(id -> {
            for(var rule:SchoolData.current().rules().getOrDefault(id,List.of()))if(rule.matches(target))return rule.multiplier();
            return 1;
        });
    }
    /** Called once at the Ars Pre event boundary, after its normal flat bonus and before native mitigation. */
    public static void apply(SpellDamageEvent.Pre event) {
        if(event.isCanceled()||event.target==null||event.target.level().isClientSide||!Float.isFinite(event.damage)||event.damage<=0
                ||!dev.arsconflux.api.damage.SpellPower.useArsEvent(event.damageSource))return;
        var scoped=SCOPE.get();var weights=scoped==null?resolve(event.damageSource):scoped;
        double factor=dev.arsconflux.api.damage.SpellPower.multiplier(event.caster,weights);
        if(factor<=0){event.damage=0;return;}
        if(ConfluxConfig.schoolDamageRules())factor*=multiplier(weights,event.target);
        event.damage=(float)Math.min(Float.MAX_VALUE,event.damage*factor);
    }
    public static void unload(Level world) {PAYLOADS.keySet().removeIf(entity->entity.level()==world);}
    public static void stop() {PAYLOADS.clear();SCOPE.remove();SchoolData.clear();}
    private SchoolDamage() {}
}
