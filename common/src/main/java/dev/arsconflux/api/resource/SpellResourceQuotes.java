package dev.arsconflux.api.resource;

import com.hollingsworth.arsnouveau.api.spell.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Dry estimates for provider-owned triggered resources, separate from Ars/Hex entrypoint prices.
 * One possible first activation per paid glyph, not an upper bound on scatter, repeated hits or channels. */
public final class SpellResourceQuotes {
    private SpellResourceQuotes() {}
    public record Entry(ResourceLocation glyph, ResourceCharge charge) {
        public boolean known() { return charge != null; }
    }
    public record Estimate(List<Entry> entries) {
        public Estimate { entries = List.copyOf(entries); }
        public boolean complete() { return entries.stream().allMatch(Entry::known); }
        /** False for unknown prices or when any physical pool cannot cover its combined known costs. */
        public boolean affordable() { return complete() && knownAffordable(); }
        public boolean knownAffordable() {
            record Total(ResourceAccount account, long amount) {}
            var totals = new HashMap<Object, Total>();
            for (var entry : entries) {
                if (!entry.known() || entry.charge().account().free()) continue;
                var charge = entry.charge();
                var old = totals.get(charge.account().reservationKey());
                totals.put(charge.account().reservationKey(), new Total(charge.account(),
                        (old == null ? 0L : old.amount()) + charge.amount()));
            }
            return totals.values().stream().allMatch(total -> ResourceTransactions.canSpend(total.account(), total.amount()));
        }
    }
    public static Estimate estimate(SpellContext context) {
        var parts = ArsSpellAccess.parts(context.getSpell());
        var result = new ArrayList<Entry>();
        for (int i = 0; i < parts.size(); i++) {
            var part = parts.get(i);
            if (!(part instanceof TriggerPaidGlyph paid)) continue;
            var augments = new ArrayList<AbstractAugment>();
            // A replacement augment belongs to the cast method; include its sibling augments on both sides.
            int start = i;
            if (part instanceof AbstractAugment) while (start > 0 && parts.get(start - 1) instanceof AbstractAugment) start--;
            for (int j = part instanceof AbstractAugment ? start : i + 1;
                 j < parts.size() && parts.get(j) instanceof AbstractAugment; j++)
                if (parts.get(j) != part) augments.add((AbstractAugment)parts.get(j));
            result.add(new Entry(part.getRegistryName(), paid.firstCharge(context, List.copyOf(augments))));
        }
        return new Estimate(result);
    }
}
