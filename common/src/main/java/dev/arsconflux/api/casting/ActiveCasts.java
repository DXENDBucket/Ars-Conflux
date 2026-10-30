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
import java.util.function.Consumer;

/** One preparation and release per explicit active use. Hits and passive resolvers never queue chants. */
public final class ActiveCasts {
    private static final Map<ResourceLocation, ActiveCastProvider> PROVIDERS = new java.util.concurrent.ConcurrentSkipListMap<>(Comparator.comparing(ResourceLocation::toString));
    private static final Map<UUID, Pending> ACTIVE = new HashMap<>();
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();
    public enum Deferral { UNNEEDED, QUEUED, DENIED }
    private record Participant(ActiveCastProvider provider, ActiveCastRequest request) {}
    private record Pending(ActiveCastRequest request, ServerLevel world, InteractionHand hand, ItemStack held,
                           ItemStack snapshot, UUID token, long start, int ticks, List<Participant> participants,
                           Runnable release, ActiveCastGuard guard) {
        boolean valid() {
            var caster = request.caster();
            return caster.isAlive() && !caster.isRemoved() && caster.level() == world
                    && (!(caster instanceof ServerPlayer player) || !player.hasDisconnected() && !player.isSpectator())
                    && (!guard.retainHandItem() || caster.getItemInHand(hand) == held && ItemStack.matches(held, snapshot))
                    && guard.valid().getAsBoolean()
                    && participants.stream().allMatch(p -> p.provider.chantingEnabled(p.request));
        }
        void sync(int remaining) { for (var p : participants) p.provider.chantProgress(p.request, token, ticks, remaining); }
    }
    private static final class Attempt {
        final ActiveCastRequest request;
        final ActiveCastPolicy policy;
        final LinkedHashSet<Spell> recipes = new LinkedHashSet<>();
        final LinkedHashMap<ResourceLocation, Runnable> releaseActions = new LinkedHashMap<>();
        boolean released;
        boolean admittedSequence;
        Attempt(ActiveCastRequest request, ActiveCastPolicy policy) { this.request = request; this.policy = policy; recipes.add(request.spell()); }
    }
    private ActiveCasts() {}
    public static void register(ResourceLocation id, ActiveCastProvider provider) {
        if (PROVIDERS.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(provider)) != null)
            throw new IllegalStateException("Duplicate active-cast provider: " + id);
    }
    private static List<ActiveCastProvider> providers(Spell spell) {
        return PROVIDERS.values().stream().filter(p -> p.supports(spell)).toList();
    }
    /** Null outside a matching active transaction; entrypoints can carry its policy to derived resolvers. */
    public static ActiveCastPolicy currentPolicy(LivingEntity caster) {
        var attempt = CURRENT.get();
        return attempt != null && attempt.request.caster() == caster ? attempt.policy : null;
    }
    /** Native result-specific cooldown implementations should consult this inside their cast/session scope. */
    public static boolean cooldownsEnabled(LivingEntity caster) {
        var attempt = CURRENT.get();
        if (attempt != null && attempt.request.caster() == caster) return attempt.policy.cooldowns();
        var context = dev.arsconflux.api.context.CastContexts.current();
        return context == null || context.caster() != caster || ActiveCastPolicy.of(context.ars()).cooldowns();
    }
    public static boolean allowed(Spell spell, LivingEntity caster, ActiveCastSource source) {
        if (caster == null || !(caster.level() instanceof ServerLevel)) return true;
        var rejected = rejection(spell, caster, source);
        if (rejected == null) return true;
        if (rejected.message() != null && caster instanceof ServerPlayer player)
            player.displayClientMessage(rejected.message().copy().withStyle(net.minecraft.ChatFormatting.RED), true);
        return false;
    }
    /** Quiet for structured providers, suitable for previews/AI. Longest known blocker wins;
     * equal/unknown durations retain stable provider order. */
    public static CastRejection rejection(Spell spell, LivingEntity caster, ActiveCastSource source) {
        if (caster == null || !(caster.level() instanceof ServerLevel) || !cooldownsEnabled(caster)) return null;
        var request = new ActiveCastRequest(spell, caster, source);
        CastRejection selected = null;
        for (var provider : providers(spell)) {
            var candidate = provider.rejection(request);
            if (candidate != null && (selected == null || candidate.remainingTicks() > selected.remainingTicks())) selected = candidate;
        }
        return selected;
    }
    /** Defer a result-dependent commit until the enclosing active use is acknowledged.
     * Keys deduplicate repeated successes within that use. Nothing runs for an unacknowledged
     * use. False means no matching active use exists; the caller may commit immediately.
     * This does not create an active use or change passive/ongoing trigger semantics. */
    public static boolean afterRelease(LivingEntity caster, ResourceLocation key, Runnable action) {
        Objects.requireNonNull(key); Objects.requireNonNull(action);
        var attempt = CURRENT.get();
        if (!cooldownsEnabled(caster)) return true; // Handled, intentionally not committed for this chain.
        if (attempt == null || attempt.request.caster() != caster) return false;
        if (attempt.policy.cooldowns()) attempt.releaseActions.putIfAbsent(key, action);
        return true;
    }
    public static <T> T perform(Spell spell, LivingEntity caster, InteractionHand hand, ActiveCastSource source,
                                Supplier<T> action, T denied, T queued) {
        return perform(spell, caster, hand, source, ActiveCastPolicy.DEFAULT, action, denied, queued);
    }
    public static <T> T perform(Spell spell, LivingEntity caster, InteractionHand hand, ActiveCastSource source,
                                ActiveCastPolicy policy, Supplier<T> action, T denied, T queued) {
        Objects.requireNonNull(policy);
        var current = CURRENT.get();
        if (current != null && current.request.caster() == caster) return execute(spell, caster, source, current.policy, action, denied);
        if (policy.cooldowns() && !allowed(spell, caster, source)) return denied;
        Runnable release = () -> execute(spell, caster, source, policy, action, denied);
        return switch (policy.chanting() ? defer(spell, caster, hand, source, policy, release) : Deferral.UNNEEDED) {
            case QUEUED -> queued;
            case DENIED -> denied;
            case UNNEEDED -> execute(spell, caster, source, policy, action, denied);
        };
    }
    public static Deferral defer(Spell spell, LivingEntity caster, InteractionHand hand, ActiveCastSource source, Runnable release) {
        return defer(spell, caster, hand, source, ActiveCastPolicy.DEFAULT, release);
    }
    private static Deferral defer(Spell spell, LivingEntity caster, InteractionHand hand, ActiveCastSource source, ActiveCastPolicy policy, Runnable release) {
        return defer(spell, caster, hand, source, policy, release, ActiveCastGuard.DEFAULT);
    }
    private static Deferral defer(Spell spell, LivingEntity caster, InteractionHand hand, ActiveCastSource source,
                                  ActiveCastPolicy policy, Runnable release, ActiveCastGuard guard) {
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
        int ticks = CastingTiming.chantTicks(caster,
                ChantDurations.combine(durations, dev.arsconflux.config.ConfluxConfig.sumChants()));
        if (ticks == 0) return Deferral.UNNEEDED;
        if (!caster.isAlive() || ACTIVE.size() >= 4096 || policy.cooldowns() && !allowed(spell, caster, source)) return Deferral.DENIED;
        var held = caster.getItemInHand(hand);
        var pending = new Pending(request, world, hand, held, held.copy(), UUID.randomUUID(), world.getGameTime(), ticks, participants, release, guard);
        ACTIVE.put(caster.getUUID(), pending);
        pending.sync(ticks);
        return Deferral.QUEUED;
    }
    public static boolean isChanting(LivingEntity caster) {
        var pending = caster == null ? null : ACTIVE.get(caster.getUUID());
        return pending != null && pending.request.caster() == caster;
    }
    public static <T> T execute(Spell spell, LivingEntity caster, ActiveCastSource source, Supplier<T> action, T denied) {
        var current = CURRENT.get();
        return execute(spell, caster, source, current != null && current.request.caster() == caster ? current.policy : ActiveCastPolicy.DEFAULT, action, denied);
    }
    private static <T> T execute(Spell spell, LivingEntity caster, ActiveCastSource source, ActiveCastPolicy policy, Supplier<T> action, T denied) {
        if (caster == null || !(caster.level() instanceof ServerLevel)) return action.get();
        if (policy.cooldowns() && !allowed(spell, caster, source)) return denied;
        var previous = CURRENT.get();
        if (previous != null && previous.request.caster() == caster) return action.get();
        var attempt = new Attempt(new ActiveCastRequest(spell, caster, source), policy);
        CURRENT.set(attempt);
        T result;
        try { result = action.get(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
        if (attempt.released && attempt.policy.cooldowns()) {
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
        if (attempt != null && ArsSpellAccess.caster(resolver.spellContext) == attempt.request.caster())
            attempt.policy.attach(resolver.spellContext);
        return attempt == null || !attempt.policy.cooldowns() || attempt.admittedSequence || ArsSpellAccess.caster(resolver.spellContext) != attempt.request.caster()
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

    /** One admitted active use. Its later phases retain native effect/resource rules, but do
     * not repeat preparation or provider-wide activation cooldowns. Caller owns cancellation. */
    public static final class Sequence implements AutoCloseable {
        private final List<Spell> plan;
        private final ActiveCastRequest request;
        private final ActiveCastPolicy policy;
        private final ServerLevel world;
        private final ActiveUseCommit commit = new ActiveUseCommit();
        private Sequence(List<Spell> plan, ActiveCastRequest request, ActiveCastPolicy policy, ServerLevel world) {
            this.plan = List.copyOf(plan); this.request = request; this.policy = policy; this.world = world;
        }
        public boolean open() { return commit.open(); }
        public ActiveCastPolicy policy() { return policy; }
        @Override public void close() { commit.close(); }
    }

    /** Prepare all declared phases together without executing a synthetic multi-form spell. */
    public static Deferral prepareSequence(List<Spell> phases, LivingEntity caster, InteractionHand hand,
            ActiveCastSource source, ActiveCastPolicy policy, ActiveCastGuard guard, Consumer<Sequence> release) {
        if (caster == null || !(caster.level() instanceof ServerLevel world)) return Deferral.DENIED;
        var previous = ACTIVE.get(caster.getUUID());
        if (previous != null) {
            if (previous.valid()) return Deferral.DENIED;
            cancel(previous);
        }
        var plan = List.copyOf(phases);
        var parts = new ArrayList<com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart>();
        for (var phase : plan) parts.addAll(ArsSpellAccess.parts(phase));
        // Providers only inspect this aggregate for timing/rejection; it is never validated or cast.
        var aggregate = new Spell(parts);
        if (policy.cooldowns() && !allowed(aggregate, caster, source)) return Deferral.DENIED;
        var request = new ActiveCastRequest(aggregate, caster, source);
        Runnable start = () -> {
            if (!caster.isAlive() || caster.level() != world
                    || policy.cooldowns() && !allowed(aggregate, caster, source)) return;
            release.accept(new Sequence(plan, request, policy, world));
        };
        var result = policy.chanting() ? defer(aggregate, caster, hand, source, policy, start, guard) : Deferral.UNNEEDED;
        if (result == Deferral.UNNEEDED) start.run();
        return result;
    }

    /** Dispatch one phase of an admitted use. A failed dispatch never acknowledges activation. */
    public static <T> T executeSequence(Sequence sequence, Spell phase, Supplier<T> action, T denied) {
        var caster = sequence.request.caster();
        if (!sequence.open() || !caster.isAlive() || caster.level() != sequence.world) return denied;
        var previous = CURRENT.get();
        var attempt = new Attempt(new ActiveCastRequest(phase, caster, sequence.request.source()), sequence.policy);
        attempt.admittedSequence = true;
        CURRENT.set(attempt);
        T result;
        try { result = action.get(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
        if (attempt.released && sequence.policy.cooldowns()) {
            try {
                sequence.commit.acknowledge(true, () -> {
                    for (var provider : PROVIDERS.values()) if (sequence.plan.stream().anyMatch(provider::supports))
                        provider.released(sequence.request, sequence.plan);
                });
            } finally {
                for (var after : attempt.releaseActions.values()) after.run();
            }
        }
        return result;
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
