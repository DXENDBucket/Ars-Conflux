package dev.arsconflux.internal.billing;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.resource.*;
import dev.arsconflux.config.ConfluxConfig;
import dev.arsconflux.platform.ArsAccess;
import dev.arsconflux.mixin.SpellResolverAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import java.util.*;

/** Optional adapter for native Ars effect dispatch. Addons keep their own payment contract. */
public final class VanillaTriggerBilling {
    private static final String QUOTE = "ars_conflux_trigger_quote";
    private static final String RAW = "ars_conflux_trigger_raw_cost";
    private static final String DISCOUNT = "ars_conflux_trigger_discount";
    private static final String SNAPSHOT = "ars_conflux_trigger_billing";
    private static final String FROZEN = "ars_conflux_quote_frozen";
    private static final String REQUIRED = "ars_conflux_quote_required";
    private static final String SPENT = "ars_conflux_quote_spent";
    private record Group(int index, AbstractSpellPart part, List<AbstractSpellPart> parts, int weight, boolean deferred) {}
    private VanillaTriggerBilling() {}

    public static boolean enabled(SpellContext context) {
        boolean mode = context.tag.contains(SNAPSHOT) ? context.tag.getBoolean(SNAPSHOT) : ConfluxConfig.vanillaTriggerBilling();
        return mode && context.getCaster() instanceof LivingCaster
                && !context.tag.getBoolean(QUOTE);
    }
    public static void captureMode(SpellContext context) {
        if (!context.tag.contains(SNAPSHOT)) context.tag.putBoolean(SNAPSHOT, ConfluxConfig.vanillaTriggerBilling());
    }
    public static Integer frozenQuoteCost(SpellContext context, boolean expended) {
        return context.tag.getBoolean(QUOTE) && context.tag.getBoolean(FROZEN)
                ? context.tag.getInt(expended ? SPENT : REQUIRED) : null;
    }
    private static boolean deferred(AbstractSpellPart part) {
        return part instanceof AbstractEffect && !(part instanceof TriggerPaidGlyph)
                && "ars_nouveau".equals(part.getRegistryName().getNamespace());
    }
    private static List<Group> groups(SpellContext context) {
        var parts = ArsAccess.parts(context.getSpell());
        var result = new ArrayList<Group>();
        for (int i = 0; i < parts.size();) {
            int start = i; var part = parts.get(i++);
            if (part == null) continue;
            long cost = part.getCastingCost();
            while (i < parts.size() && parts.get(i) instanceof AbstractAugment augment) {
                cost += augment.getCostForPart(part); i++;
            }
            result.add(new Group(start, part, parts.subList(start, i), (int)Math.min(Integer.MAX_VALUE, Math.max(0, cost)), deferred(part)));
        }
        return result;
    }
    private static long sum(List<Group> groups) { return groups.stream().mapToLong(Group::weight).sum(); }

    /** Called before cost events so existing resource bridges see the actual launch quote. */
    public static int upfrontRaw(SpellContext context, int original) {
        if (context.tag.getBoolean(QUOTE)) return context.tag.getInt(RAW);
        if (!enabled(context)) return original;
        var groups = groups(context); long sum = sum(groups), before = 0; int cost = 0;
        for (var group : groups) {
            if (!group.deferred()) cost += CostSlices.share(Math.max(0, original), before, group.weight(), sum);
            before += group.weight();
        }
        return cost;
    }
    public static int upfrontDiscount(SpellContext context, int original) {
        if (context.tag.getBoolean(QUOTE)) return context.tag.getInt(DISCOUNT);
        return enabled(context) ? allocated(context, Math.max(0, original), -1) : original;
    }
    private static int allocated(SpellContext context, int total, int index) {
        var groups = groups(context); long sum = sum(groups), before = 0; int amount = 0;
        for (var group : groups) {
            if (index >= 0 ? group.index() == index : !group.deferred())
                amount += CostSlices.share(total, before, group.weight(), sum);
            before += group.weight();
        }
        return amount;
    }
    public static boolean hasDeferred(SpellContext context) {
        return enabled(context) && ArsAccess.parts(context.getSpell()).stream().anyMatch(VanillaTriggerBilling::deferred);
    }

    /** Ars effects return void: normal dispatch is a paid attempt, not a claim of world mutation. */
    public static void resolve(AbstractEffect effect, SpellResolver resolver, HitResult hit, Runnable action) {
        var context = resolver.spellContext;
        if (!enabled(context) || !deferred(effect)) { action.run(); return; }
        if (hit == null || hit.getType() == HitResult.Type.MISS) { context.stop(); return; }
        int index = context.getCurrentIndex() - 1;
        var group = groups(context).stream().filter(g -> g.index() == index && g.part() == effect).findFirst().orElse(null);
        if (group == null) throw new IllegalStateException("Ars trigger index does not match the executing effect");
        var quoteContext = context.clone().withSpell(new Spell(List.copyOf(group.parts())));
        quoteContext.tag = context.tag.copy();
        quoteContext.tag.putBoolean(QUOTE, true);
        quoteContext.tag.putInt(RAW, allocated(context, Math.max(0, context.getSpell().getCost()), index));
        int discount = com.hollingsworth.arsnouveau.api.util.ManaUtil.getPlayerDiscounts(
                context.getUnwrappedCaster(), resolver.spell, context.getCasterTool());
        quoteContext.tag.putInt(DISCOUNT, allocated(context, Math.max(0, discount), index));
        var quote = resolver.getNewResolver(quoteContext);
        try {
            int required = quote.getResolveCost();
            int spent = ArsAccess.spentCost(quote);
            quoteContext.tag.putInt(REQUIRED, required); quoteContext.tag.putInt(SPENT, spent);
            quoteContext.tag.putBoolean(FROZEN, true);
            // Both native cost-event phases can modify prices; never dispatch when the debit is unaffordable.
            var account = new ResourceAccount() {
                public Object reservationKey() { return new ArsManaAccount(context.getCaster()).reservationKey(); }
                public boolean free() { return context.getUnwrappedCaster() instanceof Player player && player.isCreative(); }
                public boolean canSpend(int amount) {
                    if (amount > spent) return ArsAccess.canSpend(context.getCaster(), amount);
                    return ArsAccess.canSpend(context.getCaster(), Math.max(required, spent))
                            && ((SpellResolverAccess)quote).arsConflux$enoughMana(context.getUnwrappedCaster());
                }
                public void spend(int amount) { quote.expendMana(); }
            };
            if (!new ResourceCharge(account, spent).trigger(() -> { action.run(); return true; })) context.stop();
        } finally { NativePaymentCleanup.finish(quoteContext); }
    }
}
