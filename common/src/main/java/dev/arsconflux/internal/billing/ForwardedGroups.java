package dev.arsconflux.internal.billing;

import java.util.*;
import java.util.function.Predicate;

/** Exclude a paid effect together with its written augments from an external repeater's quote. */
public final class ForwardedGroups {
    private ForwardedGroups() {}
    public static <T> List<T> unmanaged(List<T> parts,Predicate<T> augment,Predicate<T> managed,Predicate<T> effect) {
        var retained=new ArrayList<T>(); boolean hasEffect=false;
        for(int i=0;i<parts.size();) {
            var part=parts.get(i++);
            if(augment.test(part))continue;
            boolean owned=managed.test(part);
            if(!owned) { retained.add(part); hasEffect|=effect.test(part); }
            while(i<parts.size() && augment.test(parts.get(i))) {
                if(!owned)retained.add(parts.get(i)); i++;
            }
        }
        return hasEffect?List.copyOf(retained):List.of();
    }
}
