package dev.arsconflux.internal.hex;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.api.context.ResolverContexts;
import dev.arsconflux.api.context.ResolverExtension;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.internal.billing.VanillaTriggerBilling;
import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Optional Linker integration only. No Hex classes are linked or loaded when Linker is absent. */
public final class HexLinkBridge {
    private static final ResourceLocation KEY = CarrierRegistry.id("ars_conflux:hex_link_source");
    private static final String PATTERN = "io.yukkuric.hex_ars_link.env.ars.PatternResolver";
    private static final String CALLBACK = "io.yukkuric.hex_ars_link.env.hex.GlyphCallbackCastEnv";
    private HexLinkBridge() {}
    /** One new Hex operation is one active use. The VM continues; only its Ars release may
     * be deferred. Action fields retain its original target/position, not a later sight trace. */
    private static final ThreadLocal<java.util.Set<Object>> RELEASING = new ThreadLocal<>();
    public static boolean intercept(Object action, Object environment) {
        var releasing = RELEASING.get();
        if (releasing != null && releasing.contains(action)) return false;
        try {
            boolean normal = action.getClass().getName().equals("io.yukkuric.hex_ars_link.action.OpCastFromPlayer$Action");
            var resolver = normal ? null : (SpellResolver)action.getClass().getMethod("getResolver").invoke(action);
            var source = resolver == null ? null : origin(resolver);
            if (environment.getClass().getName().equals(CALLBACK)
                    || source != null && source.environment().getClass().getName().equals(CALLBACK)) return false;
            var caster = normal ? (net.minecraft.world.entity.LivingEntity)action.getClass().getMethod("getOwner").invoke(action)
                    : dev.arsconflux.api.glyph.ArsSpellAccess.caster(resolver.spellContext);
            var spell = normal ? (com.hollingsworth.arsnouveau.api.spell.Spell)action.getClass().getMethod("getSpell").invoke(action) : resolver.spell;
            if (caster == null) return false;
            var policy = new dev.arsconflux.api.casting.ActiveCastPolicy(
                    dev.arsconflux.config.ConfluxConfig.hexChanting(), dev.arsconflux.config.ConfluxConfig.hexCooldowns());
            if (resolver != null) policy.attach(resolver.spellContext);
            var method = action.getClass().getMethod("cast", Api.CONSTRUCTOR.getParameterTypes()[1]);
            dev.arsconflux.api.casting.ActiveCasts.perform(spell, caster,
                    net.minecraft.world.InteractionHand.MAIN_HAND,
                    dev.arsconflux.api.casting.ActiveCastSource.CASTER_TOOL, policy,
                    () -> { release(action, environment, method); return true; }, false, true);
            return true;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not enter Linker active cast", failure);
        }
    }
    private static void release(Object action, Object environment, Method method) {
        var releasing = RELEASING.get();
        boolean root = releasing == null;
        if (root) {
            releasing = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            RELEASING.set(releasing);
        }
        releasing.add(action);
        try { method.invoke(action, environment); }
        catch (java.lang.reflect.InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException cause) throw cause;
            if (failure.getCause() instanceof Error cause) throw cause;
            throw new IllegalStateException("Linker cast failed", failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not release Linker cast", failure);
        } finally {
            releasing.remove(action);
            if (root) RELEASING.remove();
        }
    }
    private record Origin(Object environment, int launchOmission) implements ResolverExtension {
        public ResourceLocation id() { return KEY; }
        public SpellResolver derive(SpellResolver source, SpellResolver proposed) {
            if (proposed.getClass() != SpellResolver.class) return proposed;
            return create(proposed.spellContext, environment);
        }
        public SpellResolver quote(SpellResolver source, SpellContext context) { return create(context, environment); }
    }
    public static void capture(SpellResolver resolver, Object environment, int omission) {
        ResolverContexts.attach(resolver, new Origin(environment, Math.max(0, omission)));
        var policy = dev.arsconflux.api.casting.ActiveCasts.currentPolicy(dev.arsconflux.api.glyph.ArsSpellAccess.caster(resolver.spellContext));
        if (policy != null) policy.attach(resolver.spellContext);
    }
    private static Origin origin(SpellResolver resolver) { return resolver.spellContext.getAttachment(KEY); }
    private static SpellResolver create(SpellContext context, Object env) {
        try { return (SpellResolver)Api.CONSTRUCTOR.newInstance(context, env, 0); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Could not derive Linker source resolver", failure); }
    }
    /** The artificial cast method is omitted only at launch, never again from a triggered effect. */
    public static int debit(SpellResolver resolver, int original) {
        var source = origin(resolver);
        if (source == null || !resolver.getClass().getName().equals(PATTERN)
                || VanillaTriggerBilling.frozenQuoteCost(resolver.spellContext, true) != null) return original;
        return scale(Math.max(0L, (long)original - source.launchOmission()), number(Api.MANA_RATIO));
    }
    /** Media is quoted on the complete original recipe even when Ars mana is deferred until impact. */
    public static int mediaCost(SpellResolver resolver) {
        var source = origin(resolver);
        int omitted = source == null ? 0 : source.launchOmission();
        return scale(Math.max(0L, (long)VanillaTriggerBilling.originalQuote(resolver.spellContext) - omitted), number(Api.MEDIA_RATIO));
    }
    public static Integer nextCallbackDepth(SpellResolver resolver) {
        var source = origin(resolver);
        if (source == null) return null;
        try {
            return source.environment().getClass().getName().equals(CALLBACK)
                    ? Math.addExact(1, ((Number)Api.DEPTH.invoke(source.environment())).intValue()) : 1;
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Could not read Linker callback depth", failure); }
    }
    private static int scale(long cost, double ratio) {
        return (int)Math.min(Integer.MAX_VALUE, Math.max(0, cost * ratio));
    }
    private static double number(Method method) {
        try { return ((Number)method.invoke(null)).doubleValue(); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Could not read Linker pricing configuration", failure); }
    }
    /** Lazy lookup: constructor hooks are invoked only for an installed Linker. */
    private static final class Api {
        static final Constructor<?> CONSTRUCTOR;
        static final Method MANA_RATIO, MEDIA_RATIO, DEPTH;
        static {
            try {
                var env = Class.forName("at.petrak.hexcasting.api.casting.eval.CastingEnvironment");
                CONSTRUCTOR = Class.forName(PATTERN).getConstructor(SpellContext.class, env, int.class);
                var config = Class.forName("io.yukkuric.hex_ars_link.config.LinkConfig");
                MANA_RATIO = config.getMethod("costRatePatternMana");
                MEDIA_RATIO = config.getMethod("ratioExtraMediaCasting");
                DEPTH = Class.forName(CALLBACK).getMethod("getRecursionDepth");
            } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
        }
    }
}
