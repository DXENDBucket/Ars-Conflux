package dev.arsconflux.internal.presentation;
import java.util.List;
import java.util.function.Predicate;

/** Only the first meaningful phrase can replace the native primary preview icon. */
public final class SpellPreviewSelection {
    public static <T> T payload(List<T> parts,Predicate<T> augment,Predicate<T> effect,Predicate<T> payload){
        for(int i=0;i<parts.size();){
            T action=parts.get(i++);
            if(payload.test(action))return action; // Implicit-method inscription/tool template.
            while(i<parts.size()&&augment.test(parts.get(i))){
                T modifier=parts.get(i++);if(payload.test(modifier))return modifier;
            }
            if(effect.test(action))return null;
        }
        return null;
    }
    private SpellPreviewSelection(){}
}
