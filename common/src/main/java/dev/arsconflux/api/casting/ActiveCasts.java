package dev.arsconflux.api.casting;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntitySpellArrow;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Supplier;

/** One preparation and release per explicit active use. Hits and passive resolvers never queue chants. */
public final class ActiveCasts {
    private static final Map<ResourceLocation, ActiveCastProvider> PROVIDERS = new java.util.concurrent.ConcurrentSkipListMap<>(Comparator.comparing(ResourceLocation::toString));
    private static final Map<UUID, Pending> ACTIVE = new HashMap<>();
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();
    public enum Deferral { UNNEEDED, QUEUED, DENIED }
    private record Participant(ActiveCastProvider provider, ActiveCastRequest request) {}
    private record Pending(ActiveCastRequest request, ServerLevel world, InteractionHand hand, ItemStack held,
                           ItemStack snapshot, UUID token, long start, int ticks, List<Participant> participants, Runnable release) {
        boolean valid() {
            var caster = request.caster();
            return caster.isAlive() && !caster.isRemoved() && caster.level() == world
                    && (!(caster instanceof ServerPlayer player) || !player.hasDisconnected() && !player.isSpectator())
                    && caster.getItemInHand(hand) == held && ItemStack.matches(held, snapshot)
                    && participants.stream().allMatch(p -> p.provider.chantingEnabled(p.request));
        }
        void sync(int remaining) { for (var p : participants) p.provider.chantProgress(p.request, token, ticks, remaining); }
    }
    private static final class Attempt {
        final ActiveCastRequest request;
        final LinkedHashSet<Spell> recipes = new LinkedHashSet<>();
        final LinkedHashMap<ResourceLocation, Runnable> releaseActions = new LinkedHashMap<>();
        boolean released;
        Attempt(ActiveCastRequest request) { this.request = request; recipes.add(request.spell()); }
    }
    private ActiveCasts() {}
    public static void register(ResourceLocation id, ActiveCastProvider provider) {
        if (PROVIDERS.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(provider)) != null)
            throw new IllegalStateException("Duplicate active-cast provider: " + id);
    }
    private static List<ActiveCastProvider> providers(Spell spell) {
        return PROVIDERS.values().stream().filter(p -> p.supports(spell)).toList();
    }
    public static boolean allowed(Spell spell, LivingEntity caster, ActiveCastSource source) {
        if (caster == null || !(caster.level() instanceof ServerLevel)) return true;
        var request = new ActiveCastRequest(spell, caster, source);
        for (var provider : providers(spell)) if (!provider.allowed(request)) return false;
        return true;
    }
    /** Defer a result-dependent commit until the enclosing active use is acknowledged.
     * Keys deduplicate repeated successes within that use. Nothing runs for an unacknowledged
     * use. False means no matching active use exists; the caller may commit immediately.
     * This does not create an active use or change passive/ongoing trigger semantics. */
    public static boolean afterRelease(LivingEntity caster, ResourceLocation key, Runnable action) {
        Objects.requireNonNull(key); Objects.requireNonNull(action);
        var attempt = CURRENT.get();
        if (attempt == null || attempt.request.caster() != caster) return false;
        attempt.releaseActions.putIfAbsent(key, action);
        return true;
    }
    public static <T> T perform(Spell spell, LivingEntity caster, InteractionHand hand, ActiveCastSource source,
                                Supplier<T> action, T denied, T queued) {
        var current = CURRENT.get();
        if (current != null && current.request.caster() == caster) return execute(spell, caster, source, action, denied);
        if (!allowed(spell, caster, source)) return denied;
        Runnable release = () -> execute(spell, caster, source, action, denied);
        return switch (defer(spell, caster, hand, source, release)) {
            case QUEUED -> queued;
            case DENIED -> denied;
            case UNNEEDED -> execute(spell, caster, source, action, denied);
        };
    }
    public static Deferral defer(Spell spell, LivingEntity caster, InteractionHand hand, ActiveCastSource source, Runnable release) {
        if (caster == null || !(caster.level() instanceof ServerLevel world)) return Deferral.UNNEEDED;
        var current = CURRENT.get();
        if (current != null && current.request.caster() == caster) return Deferral.UNNEEDED;
        var request = new ActiveCastRequest(spell, caster, source);
        var participants = providers(spell).stream().filter(p -> p.chantingEnabled(request)).map(p -> new Participant(p, request)).toList();
        if (participants.isEmpty()) return Deferral.UNNEEDED;
        var previous = ACTIVE.get(caster.getUUID());
        if (previous != null) {
            if (previous.valid()) return Deferral.QUEUED;
            cancel(previous);
        }
        var durations = new ArrayList<Integer>();
        for (var p : participants) durations.addAll(p.provider.chantTicks(request));
        int ticks = ChantDurations.combine(durations, dev.arsconflux.config.ConfluxConfig.sumChants());
        if (ticks == 0) return Deferral.UNNEEDED;
        if (!caster.isAlive() || ACTIVE.size() >= 4096 || !allowed(spell, caster, source)) return Deferral.DENIED;
        var held = caster.getItemInHand(hand);
        var pending = new Pending(request, world, hand, held, held.copy(), UUID.randomUUID(), world.getGameTime(), ticks, participants, release);
        ACTIVE.put(caster.getUUID(), pending);
        pending.sync(ticks);
        return Deferral.QUEUED;
    }
    public static boolean isChanting(LivingEntity caster) {
        var pending = caster == null ? null : ACTIVE.get(caster.getUUID());
        return pending != null && pending.request.caster() == caster;
    }
    public static <T> T execute(Spell spell, LivingEntity caster, ActiveCastSource source, Supplier<T> action, T denied) {
        if (caster == null || !(caster.level() instanceof ServerLevel)) return action.get();
        if (!allowed(spell, caster, source)) return denied;
        var previous = CURRENT.get();
        if (previous != null && previous.request.caster() == caster) return action.get();
        var attempt = new Attempt(new ActiveCastRequest(spell, caster, source));
        CURRENT.set(attempt);
        T result;
        try { result = action.get(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
        if (attempt.released) {
            var recipes = List.copyOf(attempt.recipes);
            try {
                for (var provider : PROVIDERS.values()) if (recipes.stream().anyMatch(provider::supports))
                    provider.released(attempt.request, recipes);
            } finally {
                for (var actionAfterRelease : attempt.releaseActions.values()) actionAfterRelease.run();
            }
        }
        return result;
    }
    public static boolean allowDispatch(SpellResolver resolver) {
        var attempt = CURRENT.get();
        return attempt == null || ArsSpellAccess.caster(resolver.spellContext) != attempt.request.caster()
                || allowed(resolver.spell, attempt.request.caster(), attempt.request.source());
    }
    public static void dispatched(SpellResolver resolver, boolean success) {
        var attempt = CURRENT.get();
        if (success && attempt != null && ArsSpellAccess.caster(resolver.spellContext) == attempt.request.caster()) {
            attempt.recipes.add(resolver.spell); attempt.released = true;
        }
    }
    public static void arrowSpawned(net.minecraft.world.entity.Entity entity, boolean success) {
        if (success && CURRENT.get() != null && entity instanceof EntitySpellArrow arrow && ArsSpellAccess.resolver(arrow) != null)
            dispatched(ArsSpellAccess.resolver(arrow), true);
    }
    private static void cancel(Pending pending) { ACTIVE.remove(pending.request.caster().getUUID(), pending); pending.sync(0); }
    public static void tick() {
        for (var pending : List.copyOf(ACTIVE.values())) {
            if (!pending.valid()) { cancel(pending); continue; }
            long elapsed = pending.world.getGameTime() - pending.start;
            if (elapsed >= pending.ticks) { cancel(pending); pending.release.run(); }
            else if (elapsed % 20 == 0) pending.sync((int)(pending.ticks - elapsed));
        }
    }
    public static void stop() { for (var pending : List.copyOf(ACTIVE.values())) cancel(pending); }
    public static void unload(net.minecraft.world.level.LevelAccessor level) {
        for (var pending : List.copyOf(ACTIVE.values())) if (pending.world == level) cancel(pending);
    }
}
