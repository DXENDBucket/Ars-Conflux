package dev.arsconflux.api.execution;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Single server-thread lifecycle owner for all participating addons. */
public final class ExecutionSessions {
    private static final Map<UUID, ExecutionSession<?>> ACTIVE = new LinkedHashMap<>();
    private static final ThreadLocal<ExecutionSession<?>> CURRENT = new ThreadLocal<>();
    private ExecutionSessions() {}
    public static ExecutionSession<?> current() { return CURRENT.get(); }
    static void scoped(ExecutionSession<?> session, Runnable action) {
        var previous = CURRENT.get(); CURRENT.set(session);
        try { action.run(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    public static boolean isCasting(LivingEntity actor) {
        return ACTIVE.values().stream().anyMatch(s -> s.active() && s.caster() == actor && s.occupiesCaster());
    }
    public static boolean start(ExecutionSession<?> session) {
        if (ACTIVE.size() >= 4096 || !session.start()) {
            session.finish(ExecutionSession.EndReason.SPAWN_FAILED); return false;
        }
        ACTIVE.put(session.id(), session); return true;
    }
    public static void tick() {
        for (var session : List.copyOf(ACTIVE.values())) {
            try { session.tick(); } finally { if (!session.active()) ACTIVE.remove(session.id()); }
        }
    }
    public static void unload(ServerLevel world) { end(world, ExecutionSession.EndReason.WORLD_UNLOADED); }
    public static void stop() { end(null, ExecutionSession.EndReason.SERVER_STOPPED); }
    private static void end(ServerLevel world, ExecutionSession.EndReason reason) {
        RuntimeException first = null;
        for (var session : List.copyOf(ACTIVE.values())) if (world == null || session.usesWorld(world)) {
            try { session.finish(reason); }
            catch (RuntimeException error) { if (first == null) first = error; else first.addSuppressed(error); }
            finally { ACTIVE.remove(session.id()); }
        }
        if (first != null) throw first;
    }
}
