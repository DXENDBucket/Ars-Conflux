package dev.arsconflux.api.casting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Opt-in deadline storage and Clone handling. Native capability stores remain provider-owned.
 * Namespaces retain their existing NBT keys; duration, creative rules and commit phase stay in the addon. */
public final class PersistentCooldowns {
    private static final Map<ResourceLocation, Namespace> STORES = new LinkedHashMap<>();
    private PersistentCooldowns() {}
    public static synchronized Namespace register(ResourceLocation id, String dataKey) {
        Objects.requireNonNull(id); Objects.requireNonNull(dataKey);
        if (dataKey.isBlank()) throw new IllegalArgumentException("Empty cooldown storage key");
        if (STORES.containsKey(id) || STORES.values().stream().anyMatch(n -> n.dataKey.equals(dataKey)))
            throw new IllegalStateException("Duplicate cooldown storage: " + id);
        var store = new Namespace(id, dataKey); STORES.put(id, store); return store;
    }
    public static void copyOnClone(LivingEntity original, LivingEntity replacement, boolean death) {
        if (!(replacement.level() instanceof ServerLevel)) return;
        for (var store : List.copyOf(STORES.values())) {
            if (death && !dev.arsconflux.config.ConfluxConfig.preserveCooldownsOnDeath())
                replacement.getPersistentData().remove(store.dataKey);
            else store.copyState(original, replacement);
        }
    }
    public static final class Namespace {
        private final ResourceLocation id;
        private final String dataKey;
        private Namespace(ResourceLocation id, String dataKey) { this.id = id; this.dataKey = dataKey; }
        private long now(LivingEntity actor) { return actor.level().getServer().overworld().getGameTime(); }
        public long remaining(LivingEntity actor, String entry) {
            if (actor == null || !(actor.level() instanceof ServerLevel)) return 0;
            return Math.max(0, actor.getPersistentData().getCompound(dataKey).getLong(entry) - now(actor));
        }
        /** Start/extend a cooldown from provider-effective ticks, applying Conflux reduction once.
         * Do not pre-scale this input. Existing deadlines are never re-evaluated or shortened. */
        public void extend(LivingEntity actor, String entry, long ticks) {
            extendRaw(actor, entry, CastingTiming.cooldownTicks(actor, ticks));
        }
        /** Raw deadline operation for restoration or callers with an explicitly final duration. */
        public void extendRaw(LivingEntity actor, String entry, long ticks) {
            if (actor == null || !(actor.level() instanceof ServerLevel) || ticks <= 0) return;
            var data = actor.getPersistentData().getCompound(dataKey);
            long clock = now(actor), deadline = ticks > Long.MAX_VALUE-clock ? Long.MAX_VALUE : clock+ticks;
            data.putLong(entry, Math.max(data.getLong(entry), deadline));
            actor.getPersistentData().put(dataKey, data);
        }
        /** Result cooldowns wait for synchronous branches to finish, or commit immediately on later impacts.
         * The key includes the caller-selected phase, so grant and recovery do not swallow one another. */
        public void commit(LivingEntity actor, String entry, String phase, long ticks) {
            if (!ActiveCasts.cooldownsEnabled(actor) || ticks <= 0) return;
            long adjusted = CastingTiming.cooldownTicks(actor, ticks);
            if (adjusted <= 0) return;
            var key = ResourceLocation.tryParse(id.getNamespace()+":"+id.getPath()+"/"+entry+"/"+phase+"/"+adjusted);
            Objects.requireNonNull(key, "Invalid cooldown entry/phase");
            if (!ActiveCasts.afterRelease(actor, key, () -> extendRaw(actor, entry, adjusted))) extendRaw(actor, entry, adjusted);
        }
        /** Explicit state copy for external callers; the event hook additionally applies the death policy. */
        public void copyState(LivingEntity original, LivingEntity replacement) {
            var old = original.getPersistentData().getCompound(dataKey);
            var target = replacement.getPersistentData().getCompound(dataKey);
            for (var entry : old.getAllKeys()) target.putLong(entry, Math.max(target.getLong(entry), old.getLong(entry)));
            if (!target.isEmpty()) replacement.getPersistentData().put(dataKey, target);
        }
    }
}
