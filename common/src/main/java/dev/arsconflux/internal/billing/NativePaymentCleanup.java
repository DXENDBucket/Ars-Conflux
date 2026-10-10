package dev.arsconflux.internal.billing;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import java.lang.reflect.Method;

/** Optional resource bridge cleanup; no bridge or Iron classes are linked into Core. */
final class NativePaymentCleanup {
    private static final Method FINISH = findFinish();
    private static Method findFinish() {
        try {
            return Class.forName("com.otectus.arsnspells.casting.ArsCastPayments", false,
                    NativePaymentCleanup.class.getClassLoader()).getMethod("finish", SpellContext.class);
        } catch (ClassNotFoundException | NoSuchMethodException absent) { return null; }
    }
    static void finish(SpellContext context) {
        if (FINISH == null) return;
        try { FINISH.invoke(null, context); }
        catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not release native Ars payment quote", failure);
        }
    }
    private NativePaymentCleanup() {}
}
