package dev.arsconflux.internal.zero;
import com.github.ars_zero.common.glyph.convergence.EffectConvergence;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.IContextAttachment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class ZeroSourceBilling {
    private static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(
            "ars_conflux", "zero_convergence_source_budget");

    private ZeroSourceBilling() {}

    /** Record the source quote at dispatch, before a turret changes phase or a projectile travels. */
    public static void prepareSourceCast(SpellContext context, int paidSource) {
        if (context.getCaster() instanceof TileCaster && context.getAttachment(KEY) == null
                && context.getSpell().unsafeList().stream().anyMatch(EffectConvergence.class::isInstance)) {
            context.attachments.put(KEY, new Allowance(createBudget(context, Math.max(0, paidSource))));
        }
    }

    public static int claimInitialSource(SpellResolver resolver, EffectConvergence convergence) {
        SpellContext context = resolver.spellContext;
        List<AbstractSpellPart> parts = context.getSpell().unsafeList();
        int index = context.getCurrentIndex() - 1;
        if (index < 0 || index >= parts.size() || parts.get(index) != convergence) {
            return 0;
        }
        // Branches may have been cloned before any Convergence resolved. Resolve the allowance
        // through the common root resolver rather than lazily creating one per cloned context.
        SpellResolver root = resolver;
        Set<SpellResolver> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (root.previousResolver != null) {
            if (!visited.add(root)) {
                return 0;
            }
            root = root.previousResolver;
        }
        SpellContext rootContext = root.spellContext;
        Allowance allowance = rootContext.getAttachment(KEY);
        if (allowance == null) {
            // Unknown source-powered entrypoints must not mint source using a later, mutable
            // turret quote. Their charger can still transfer existing source normally.
            allowance = new Allowance(createBudget(rootContext, 0));
            rootContext.attachments.put(KEY, allowance);
        }
        return allowance.budget().claim(occurrenceCost(parts, index));
    }

    private static ZeroSourceBudget createBudget(SpellContext context, int paidSource) {
        List<AbstractSpellPart> parts = context.getSpell().unsafeList();
        long convergenceCost = 0;
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i) instanceof EffectConvergence) {
                convergenceCost += occurrenceCost(parts, i);
            }
        }
        int rawConvergenceCost = (int) Math.min(Integer.MAX_VALUE, convergenceCost);
        if (context.getCaster() instanceof TileCaster) {
            return ZeroSourceBudget.sourcePowered(rawConvergenceCost,
                    context.getSpell().getCost(), paidSource);
        }
        return ZeroSourceBudget.manaPowered(rawConvergenceCost);
    }

    private static int occurrenceCost(List<AbstractSpellPart> parts, int index) {
        AbstractSpellPart effect = parts.get(index);
        long cost = effect.getCastingCost();
        for (int i = index + 1; i < parts.size() && parts.get(i) instanceof AbstractAugment; i++) {
            cost += ((AbstractAugment) parts.get(i)).getCostForPart(effect);
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, cost));
    }

    private record Allowance(ZeroSourceBudget budget) implements IContextAttachment {
        @Override
        public ResourceLocation id() {
            return KEY;
        }
    }
}
