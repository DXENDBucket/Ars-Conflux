package dev.arsconflux.internal.neg;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.api.context.ResolverContexts;
import dev.arsconflux.api.context.ResolverExtension;
import dev.arsconflux.api.projectile.CarrierRegistry;
import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.Constructor;

/** Preserve NEG's focus-thread resolver without linking an optional addon at startup. */
public final class NegResolverBridge {
    private static final ResourceLocation KEY = CarrierRegistry.id("ars_conflux:neg_binder_source");
    private NegResolverBridge() {}
    private record Origin() implements ResolverExtension {
        public ResourceLocation id() { return KEY; }
        public SpellResolver derive(SpellResolver source, SpellResolver proposed) {
            // Other integrations and collision-only resolvers keep their own execution behavior.
            return proposed.getClass() == SpellResolver.class ? create(proposed.spellContext) : proposed;
        }
        public SpellResolver quote(SpellResolver source, SpellContext context) { return create(context); }
    }
    public static void capture(SpellResolver resolver) { ResolverContexts.attach(resolver, new Origin()); }
    private static SpellResolver create(SpellContext context) {
        try { return (SpellResolver)Api.CONSTRUCTOR.newInstance(context); }
        catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not derive NEG's focus-thread resolver", failure);
        }
    }
    private static final class Api {
        static final Constructor<?> CONSTRUCTOR;
        static {
            try {
                CONSTRUCTOR = Class.forName("alexthw.not_enough_glyphs.api.ThreadwiseSpellResolver")
                        .getConstructor(SpellContext.class);
            } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
        }
    }
}
