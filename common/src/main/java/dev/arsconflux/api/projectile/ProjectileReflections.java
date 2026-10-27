package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.damage.NativeSpellDamage;
import dev.arsconflux.api.execution.ExecutionSession;
import dev.arsconflux.internal.projectile.ReflectionRules;
import dev.arsconflux.internal.projectile.ReflectionStamp;
import dev.arsconflux.platform.ArsAccess;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** Server-thread reflection of an opt-in flight. Native, unmanaged projectiles remain provider-owned. */
public final class ProjectileReflections {
    public enum Result { UNMANAGED, REFLECTED, REJECTED }
    private static final Map<Entity,WeakReference<ExecutionSession<?>>> OWNERS=new WeakHashMap<>();
    private static final ClassValue<Boolean> LINEAR=new ClassValue<>() {
        protected Boolean computeValue(Class<?> type) {
            try { return type.getMethod("tickNextPosition").getDeclaringClass()==EntityProjectileSpell.class
                    && type.getMethod("getNextHitPosition").getDeclaringClass()==EntityProjectileSpell.class; }
            catch(ReflectiveOperationException absent) { return false; }
        }
    };
    private ProjectileReflections() {}
    public static void track(Entity entity,ExecutionSession<?> session) { OWNERS.put(entity,new WeakReference<>(session)); }
    public static void untrack(Entity entity,ExecutionSession<?> session) {
        var reference=OWNERS.get(entity); if(reference!=null && reference.get()==session)OWNERS.remove(entity);
    }
    private static EntityProjectileSpell carrier(Entity entity) {
        var motion=ArsTrajectory.forVisual(entity);
        return motion!=null?motion.carrier():entity instanceof CarrierProxy proxy?proxy.confluxTrajectory():entity instanceof EntityProjectileSpell spell?spell:null;
    }
    public static Result reflect(Projectile projectile,Vec3 normal,LivingEntity reflector) {
        var carrier=carrier(projectile);
        var reference=OWNERS.get(projectile); var session=reference==null?null:reference.get();
        var motion=carrier==null?null:ArsTrajectory.managed(carrier);
        if(session==null && motion!=null)session=motion.owningSession();
        Entity subject=motion!=null && projectile==carrier?motion.visual():projectile;
        if(session==null && motion==null && !(projectile instanceof CarrierProxy))return Result.UNMANAGED;
        if(projectile.level().isClientSide || projectile.isRemoved() || normal==null
                || reflector!=null && reflector.level()!=projectile.level() || session!=null && !session.active())return Result.REJECTED;
        var velocity=projectile.getDeltaMovement();
        var bounced=ReflectionRules.reflect(velocity.x,velocity.y,velocity.z,normal.x,normal.y,normal.z);
        if(bounced==null)return Result.REJECTED;
        if(carrier!=null) {
            if(carrier instanceof ReflectableCarrier custom) { if(!custom.canReflect())return Result.REJECTED; }
            else {
                var profile=CarrierRegistry.of(carrier);
                if(!LINEAR.get(carrier.getClass()) || profile!=null && profile.repeated())return Result.REJECTED;
            }
        }
        List<Entity> group=session==null?List.of(subject):session.projectiles();
        if(!group.contains(subject) || !ReflectionRules.sameFlight(group,subject,ProjectileReflections::carrier)
                || group.stream().anyMatch(entity->entity.level()!=projectile.level()))return Result.REJECTED;
        var owner=reflector!=null?reflector:projectile.getOwner() instanceof LivingEntity living?living:null;
        var previousOwner=projectile.getOwner() instanceof LivingEntity living?living:null;
        var reflected=new Vec3(bounced.x(),bounced.y(),bounced.z());
        var displacement=reflected.normalize().scale(.3);
        SpellResolver replacement=null;
        if(carrier!=null && owner!=null) {
            var resolver=ArsAccess.resolver(carrier);
            if(resolver!=null) {
                var frame=CastContexts.reflected(CastContext.of(resolver.spellContext,null,null),owner);
                replacement=resolver.getNewResolver(frame.ars());
            }
        }
        if(session!=null && owner!=null)session.reflectDamageOwner(owner);
        // Every member of a native salvo follows the same carrier. Preserve formation offsets.
        for(var entity:group) {
            entity.setPos(entity.position().add(displacement)); entity.setDeltaMovement(reflected); entity.hasImpulse=true;
            if(owner!=null && entity instanceof Projectile member)member.setOwner(owner);
            var frame=NativeSpellDamage.context(entity);
            if(frame!=null && owner!=null)NativeSpellDamage.bind(entity,CastContexts.reflected(frame,owner));
            orient(entity,reflected);
        }
        if(carrier!=null) {
            if(!group.contains(carrier))carrier.setPos(carrier.position().add(displacement));
            carrier.setDeltaMovement(reflected); carrier.hasImpulse=true;
            ArsAccess.reflected(carrier);
            if(carrier instanceof ReflectionStamp stamp)stamp.conflux$markReflection();
            if(owner!=null)carrier.setOwner(owner);
            if(replacement!=null)ArsAccess.resolver(carrier,replacement);
            if(carrier instanceof ReflectableCarrier custom)custom.reflected(previousOwner,owner);
            orient(carrier,reflected);
        }
        return Result.REFLECTED;
    }
    private static void orient(Entity entity,Vec3 velocity) {
        entity.setYRot((float)Math.toDegrees(Math.atan2(velocity.x,velocity.z)));
        entity.setXRot((float)Math.toDegrees(Math.atan2(velocity.y,velocity.horizontalDistance())));
    }
    /** Preserve the native DamageSource object/type; only credit an explicit reflected resolution. */
    public static Entity damageOwner(Entity declared) {
        var frame=CastContexts.current();
        return frame!=null && CastContexts.isReflected(frame.ars())
                ?ReflectionRules.damageOwner(declared,frame.caster(),frame.executor(),frame.damageOwner()):declared;
    }
}
