package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.api.spell.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Reduce count before Ars generates its symmetric fan. Each payload supplies its actual price. */
public final class VolleyBudget {
    private record Batch(SpellResolver resolver, Map<SpellStats, Integer> counts) {}
    private static final ThreadLocal<Deque<Batch>> BATCHES = ThreadLocal.withInitial(ArrayDeque::new);
    private VolleyBudget() {}
    public static void begin(SpellResolver resolver) {
        if (ArsSpellAccess.level(resolver.spellContext) instanceof ServerLevel)
            BATCHES.get().push(new Batch(resolver, new IdentityHashMap<>()));
    }
    public static int splits(SpellStats stats, int requested) {
        var stack = BATCHES.get();
        if (stack.isEmpty()) return requested;
        var batch = stack.peek(); var resolver = batch.resolver();
        var profile = CarrierRegistry.of(resolver);
        if (profile == null || profile.repeated()) return requested;
        var form = stats.getAugments().stream().filter(ProjectileForm.class::isInstance)
                .map(ProjectileForm.class::cast).findFirst().orElse(null);
        if (form == null) return requested;
        return batch.counts().computeIfAbsent(stats, ignored -> {
            var charge = form.initialCharge(resolver.spellContext,
                    resolver.spell.getAugments(0, ArsSpellAccess.caster(resolver.spellContext)));
            return charge == null ? requested : charge.affordableCount(requested + 1) - 1;
        });
    }
    public static void finish(SpellResolver resolver) {
        var stack = BATCHES.get();
        if (!stack.isEmpty() && stack.peek().resolver() == resolver) stack.pop();
        if (stack.isEmpty()) BATCHES.remove();
    }
}
