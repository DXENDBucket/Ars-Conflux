package dev.arsconflux.api.context;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import java.util.List;

/** Public source-extension bridge, independent of any particular spell addon or external entrypoint. */
public final class ResolverContexts {
    private ResolverContexts() {}
    private static List<ResolverExtension> extensions(SpellResolver resolver) {
        return resolver.spellContext.attachments.values().stream()
                .filter(ResolverExtension.class::isInstance).map(ResolverExtension.class::cast).toList();
    }
    public static void attach(SpellResolver resolver, ResolverExtension extension) {
        resolver.spellContext.attachments.put(extension.id(), extension);
    }
    /** Copy source extensions without replacing a custom resolver's execution behavior. */
    public static <T extends SpellResolver> T inherit(SpellResolver source, T result) {
        dev.arsconflux.api.casting.ActiveCastPolicy.inherit(source.spellContext, result.spellContext);
        for (var extension : extensions(source))
            result.spellContext.attachments.putIfAbsent(extension.id(), extension);
        return result;
    }
    /** Used by Core's Ars derivation hook. Existing custom resolver implementations remain in control. */
    public static SpellResolver derived(SpellResolver source, SpellResolver proposed) {
        var result = inherit(source, proposed);
        for (var extension : extensions(source)) result = inherit(source, extension.derive(source, result));
        if (result != proposed) result.previousResolver = source;
        return result;
    }
    /** Quote through the source policy even if a carrier installed its own collision-only resolver. */
    public static SpellResolver quote(SpellResolver source, SpellContext context) {
        SpellResolver result = null;
        for (var extension : extensions(source)) {
            var candidate = extension.quote(source, context);
            if (candidate == null) continue;
            if (result != null) throw new IllegalStateException("Multiple source extensions own the native quote factory");
            result = candidate;
        }
        if (result != null) result.previousResolver = source;
        return inherit(source, result == null ? source.getNewResolver(context) : result);
    }
}
