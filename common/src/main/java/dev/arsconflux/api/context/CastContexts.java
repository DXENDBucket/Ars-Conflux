package dev.arsconflux.api.context;

import com.hollingsworth.arsnouveau.api.spell.IContextAttachment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.resource.ResourceAccount;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Transient chain attribution, carried by Ars' own cloneable attachment map. No global entity registry.
 * Executor and target belong to one activation and are deliberately not inherited by the next hit.
 * Providers choose an account at their boundary; a null account retains the attached account. */
public final class CastContexts {
    private static final ResourceLocation KEY = CarrierRegistry.id("ars_conflux:cast_origin");
    private static final ThreadLocal<CastContext> CURRENT = new ThreadLocal<>();
    private record Origin(UUID chain, LivingEntity caster, LivingEntity damageOwner, ResourceAccount account,boolean reflected) implements IContextAttachment {
        public ResourceLocation id() { return KEY; }
    }
    private CastContexts() {}
    public static CastContext current() { return CURRENT.get(); }
    public static CastContext resolve(SpellContext ars, HitResult hit, ResourceAccount account) {
        Objects.requireNonNull(ars, "Ars context");
        Origin origin = ars.getAttachment(KEY);
        var caster = origin == null ? dev.arsconflux.api.glyph.ArsSpellAccess.caster(ars) : origin.caster();
        return new CastContext(ars, caster, caster, origin == null ? caster : origin.damageOwner(), hit,
                TriggerGeometry.read(ars), account != null ? account : origin == null ? null : origin.account());
    }
    /** Explicitly attach chain ownership to a resolver before cloning or scheduling it.
     * Ars copies the attachment map; immutable origins can be shared safely between siblings. */
    public static void bind(SpellContext ars, CastContext context) {
        Objects.requireNonNull(ars); Objects.requireNonNull(context);
        Origin inherited = context.ars() == null ? null : context.ars().getAttachment(KEY);
        Origin existing = ars.getAttachment(KEY);
        UUID chain = inherited != null ? inherited.chain() : existing != null ? existing.chain() : UUID.randomUUID();
        boolean reflected=inherited!=null?inherited.reflected():existing!=null && existing.reflected();
        ars.attachments.put(KEY, new Origin(chain, context.caster(), context.damageOwner(), context.account(),reflected));
        TriggerGeometry.write(ars, context.incomingDirection());
    }
    /** Fork attribution for one flight without changing the caster, permissions, account or siblings. */
    public static CastContext reflected(CastContext original,LivingEntity owner) {
        var ars=original.ars()==null?null:original.ars().clone();
        var result=new CastContext(ars,original.caster(),original.executor(),owner,original.target(),original.incomingDirection(),original.account());
        if(ars!=null) {
            bind(ars,original);
            Origin origin=ars.getAttachment(KEY);
            ars.attachments.put(KEY,new Origin(origin.chain(),origin.caster(),owner,origin.account(),true));
        }
        return result;
    }
    public static boolean isReflected(SpellContext ars) {
        Origin origin=ars==null?null:ars.getAttachment(KEY); return origin!=null && origin.reflected();
    }
    public static boolean sameChain(SpellContext a, SpellContext b) {
        if (a == null || b == null) return false;
        if (a == b) return true;
        Origin first = a.getAttachment(KEY), second = b.getAttachment(KEY);
        return first != null && second != null && first.chain().equals(second.chain());
    }
    public static <T> T scoped(CastContext context, Supplier<T> action) {
        var previous = CURRENT.get();
        if (context == null) CURRENT.remove(); else CURRENT.set(context);
        try { return dev.arsconflux.api.damage.SpellPower.during(context, dev.arsconflux.api.school.SchoolDamage.current(), action); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    public static void run(CastContext context, Runnable action) {
        scoped(context, () -> { action.run(); return null; });
    }
}
