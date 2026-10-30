package dev.arsconflux.api.resource;

import com.hollingsworth.arsnouveau.api.spell.*;
import dev.arsconflux.platform.ArsAccess;
import java.util.*;

/** Read-only first-activation estimate, combining entry and triggered charges by physical pool. */
public final class SpellBudgets {
    public record Pool(ResourceAccount account,long amount,long capacity) {}
    public record Estimate(boolean complete,List<Pool> pools) {
        public Estimate {pools=List.copyOf(pools);}
        public boolean affordable(){return complete&&pools.stream().allMatch(p->ResourceTransactions.canSpend(p.account(),p.amount()));}
        public boolean possible(){return complete&&pools.stream().allMatch(p->p.account().free()||p.capacity()>=p.amount()
                ||p.capacity()<0&&ResourceTransactions.canSpend(p.account(),p.amount()));}
    }
    public static Estimate estimate(SpellContext context) {
        var charges=SpellResourceQuotes.estimate(context);var totals=new LinkedHashMap<Object,Pool>();
        var ars=new ArsManaAccount(context.getCaster());
        int nativeCost=dev.arsconflux.internal.billing.VanillaTriggerBilling.hasDeferred(context)
                ?dev.arsconflux.internal.billing.VanillaTriggerBilling.originalQuote(context):ArsAccess.spentCost(new SpellResolver(context));
        add(totals,ars,Math.max(0,nativeCost));
        for(var entry:charges.entries())if(entry.known())add(totals,entry.charge().account(),entry.charge().amount());
        return new Estimate(charges.complete(),List.copyOf(totals.values()));
    }
    private static void add(Map<Object,Pool> totals,ResourceAccount account,long amount) {
        var key=account.reservationKey();var old=totals.get(key);long capacity=account.capacity();
        if(old!=null&&old.capacity()>=0)capacity=capacity<0?old.capacity():Math.min(capacity,old.capacity());
        totals.put(key,new Pool(account,amount+(old==null?0:old.amount()),capacity));
    }
    private SpellBudgets() {}
}
