package dev.arsconflux.api.interaction;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.execution.ExecutionSessions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-thread input routing. It knows no spell IDs, resource types, target ranges or selection limits. */
public final class InputSessions {
    private static final Map<UUID, InputSession<?>> ENDPOINTS = new LinkedHashMap<>();
    private InputSessions() {}
    public static boolean start(InputSession<?> endpoint) {
        // One interactive input owner per actor. Background executions remain unrestricted.
        if (ENDPOINTS.values().stream().anyMatch(s -> s.execution().active()
                && s.execution().caster() == endpoint.execution().caster())) {
            endpoint.execution().finish(dev.arsconflux.api.execution.ExecutionSession.EndReason.SPAWN_FAILED);
            return false;
        }
        ENDPOINTS.put(endpoint.execution().id(), endpoint);
        try {
            if (ExecutionSessions.start(endpoint.execution())) return true;
            remove(endpoint); return false;
        } catch (RuntimeException | Error error) { remove(endpoint); throw error; }
    }
    static void remove(InputSession<?> endpoint) { ENDPOINTS.remove(endpoint.execution().id(), endpoint); }
    /** Route before chanting, mana checks and ordinary cast dispatch. */
    public static boolean route(LivingEntity actor, InteractionHand hand, Spell spell) {
        return route(actor, hand, spell, null);
    }
    public static boolean route(LivingEntity actor, InteractionHand hand, Spell spell, HitResult hit) {
        if (!(actor.level() instanceof ServerLevel)) return false;
        for (var endpoint : List.copyOf(ENDPOINTS.values())) {
            if (endpoint.execution().caster() != actor || !endpoint.execution().active()) continue;
            if (!endpoint.binding().valid()) {
                endpoint.execution().finish(dev.arsconflux.api.execution.ExecutionSession.EndReason.SOURCE_CHANGED);
                continue;
            }
            if (endpoint.binding().matches(actor, hand, spell)) {
                var input = new CastInput(actor.isShiftKeyDown() ? CastInput.Action.CANCEL : CastInput.Action.ACTIVATE,
                        hand, actor.getEyePosition(), actor.getLookAngle(), hit);
                if (endpoint.submit(input).consumed()) return true;
            } else if (endpoint.binding().owns(actor, hand)) {
                endpoint.execution().finish(dev.arsconflux.api.execution.ExecutionSession.EndReason.SOURCE_CHANGED);
            }
        }
        return false;
    }
    public static boolean waiting(LivingEntity actor) {
        return ENDPOINTS.values().stream().anyMatch(s -> s.execution().active() && s.execution().caster() == actor);
    }
}
