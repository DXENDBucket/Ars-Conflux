package dev.arsconflux.internal.controle;

import com.hollingsworth.arsnouveau.api.spell.*;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import java.util.concurrent.ThreadLocalRandom;

/** A binary operator must not share two random operands' mutable singleton state. */
public final class RandomOperands {
    private RandomOperands() {}
    public record Snapshot(double chance) implements IFilter {
        private boolean roll() { return ThreadLocalRandom.current().nextDouble() <= chance; }
        public boolean shouldResolveOnBlock(BlockHitResult hit, Level world) { return roll(); }
        public boolean shouldResolveOnEntity(EntityHitResult hit, Level world) { return roll(); }
    }
    public static Pair<Integer, Pair<IFilter, IFilter>> snapshot(SpellResolver resolver, Pair<Integer, Pair<IFilter, IFilter>> result) {
        if (result == null) return null;
        int first = resolver.spellContext.getCurrentIndex();
        int second = first + resolver.spell.getAugments(first, resolver.spellContext.getUnwrappedCaster()).size() + 1;
        return Pair.of(result.first(), Pair.of(operand(resolver, first, result.second().first()),
                operand(resolver, second, result.second().second())));
    }
    private static IFilter operand(SpellResolver resolver, int index, IFilter original) {
        var glyph = resolver.spell.get(index);
        if (!glyph.getClass().getName().equals("dev.qther.ars_controle.spell.filter.FilterRandom")) return original;
        var caster = resolver.spellContext.getUnwrappedCaster();
        var child = resolver.spellContext.makeChildContext();
        var derived = resolver.getNewResolver(child);
        var stats = new SpellStats.Builder().setAugments(resolver.spell.getAugments(index, caster))
                .addItemsFromEntity(caster).build(glyph, derived.hitResult, caster.level(), caster, child);
        try {
            // Reuse Controle's probability formula; no optional classes enter Conflux's class signatures.
            return new Snapshot((double)glyph.getClass().getMethod("calculateChance", double.class).invoke(null, stats.getAmpMultiplier()));
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Controle random-filter API changed", error); }
    }
}
