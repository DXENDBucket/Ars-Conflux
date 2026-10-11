package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.api.context.CastContexts;
import dev.arsconflux.api.execution.ExecutionSessions;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Old continuation closures may hold the origin level. Only the owning execution is retargeted. */
@Mixin(value = SpellResolver.class, remap = false)
public abstract class ExecutionWorldMixin {
    @ModifyVariable(method = "onResolveEffect", at = @At("HEAD"), argsOnly = true)
    private Level arsConflux$executionWorld(Level supplied) {
        var session = ExecutionSessions.current();
        var context = ((SpellResolver)(Object)this).spellContext;
        if (session != null && session.world() != session.originWorld()
                && (supplied == session.originWorld() || supplied == session.world())
                && CastContexts.sameChain(context, session.context().ars())) {
            ((SpellContextAccess)context).arsConflux$level(session.world());
            return session.world();
        }
        return supplied;
    }
}
