package dev.arsconflux.api.context;

import com.hollingsworth.arsnouveau.api.spell.IContextAttachment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;

/** Extra source semantics carried through resolver replacement and derivation.
 * Use immutable attachments; the integration owns the lifetime of referenced native environments.
 * This does not replace attribution, a provider's resource account, or effect implementation. */
public interface ResolverExtension extends IContextAttachment {
    /** Preserve a source-specific resolver when Ars proposes a plain derived resolver. */
    default SpellResolver derive(SpellResolver source, SpellResolver proposed) { return proposed; }
    /** Optional native quote factory. Return null to use the resolver's usual derivation. */
    default SpellResolver quote(SpellResolver source, SpellContext context) { return null; }
}
