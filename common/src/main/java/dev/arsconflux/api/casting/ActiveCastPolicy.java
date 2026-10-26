package dev.arsconflux.api.casting;

import com.hollingsworth.arsnouveau.api.spell.IContextAttachment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import dev.arsconflux.api.projectile.CarrierRegistry;
import net.minecraft.resources.ResourceLocation;

/** An entrypoint may opt out of preparation or cooldowns without changing resource payment.
 * Immutable source policy follows Ars clones and continuations, including result-based cooldowns. */
public record ActiveCastPolicy(boolean chanting, boolean cooldowns) implements IContextAttachment {
    public static final ActiveCastPolicy DEFAULT = new ActiveCastPolicy(true, true);
    private static final ResourceLocation KEY = CarrierRegistry.id("ars_conflux:active_cast_policy");
    public ResourceLocation id() { return KEY; }
    public void attach(SpellContext context) { context.attachments.put(KEY, this); }
    public static void inherit(SpellContext source, SpellContext target) {
        ActiveCastPolicy policy = source.getAttachment(KEY);
        if (policy != null) target.attachments.putIfAbsent(KEY, policy);
    }
    public static ActiveCastPolicy of(SpellContext context) {
        ActiveCastPolicy policy = context == null ? null : context.getAttachment(KEY);
        return policy == null ? DEFAULT : policy;
    }
}
