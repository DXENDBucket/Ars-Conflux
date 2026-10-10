package dev.arsconflux.api.interaction;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.context.CastContexts;
import dev.arsconflux.api.execution.ImpactContinuation;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.HitResult;

/** A one-shot remainder captured at the current cursor. Resume never restarts preceding glyphs. */
public final class ArsContinuation implements ImpactContinuation {
    private final SpellResolver frozen;
    private final CastContext context;
    private boolean closed;
    private ArsContinuation(SpellResolver resolver) {
        frozen = resolver.clone();
        context = CastContext.of(resolver.spellContext, resolver.hitResult, null);
    }
    /** Capture before stopping the original context. The caller decides whether/when to pause it. */
    public static ArsContinuation capture(SpellResolver resolver) { return new ArsContinuation(resolver); }
    @Override public void resolve(HitResult hit) {
        if (closed) return;
        closed = true;
        if (!(ArsSpellAccess.level(frozen.spellContext) instanceof ServerLevel world)) return;
        frozen.hitResult = hit;
        var next = new CastContext(frozen.spellContext, context.caster(), context.executor(), context.damageOwner(),
                hit, context.incomingDirection(), context.account());
        CastContexts.bind(frozen.spellContext, next);
        CastContexts.run(next, () -> frozen.resume(world));
    }
    @Override public void close() { closed = true; }
}
