package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHeal;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.interaction.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Provider-independent checks: only Ars and Minecraft types, no external spell imports/registries. */
public final class InteractionCases {
    private InteractionCases() {}
    private static LivingEntity actor(GameTestHelper h) {
        var actor = h.spawn(EntityType.COW, 1, 3, 1); actor.setNoAi(true); actor.setNoGravity(true);
        actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)); return actor;
    }
    public static void inputRoutingLeaseAndContext(GameTestHelper h) {
        var actor = actor(h); var executor = actor(h);
        var recipe = new Spell(MethodSelf.INSTANCE, EffectHeal.INSTANCE);
        var received = new AtomicInteger(); var closed = new AtomicInteger();
        var frame = CastContext.detached(actor, null, null).withExecutor(executor);
        var driver = new InteractiveDriver<AtomicInteger>() {
            public boolean start(ExecutionSession<AtomicInteger> session) { return true; }
            public boolean tick(ExecutionSession<AtomicInteger> session) { return false; }
            public void close(ExecutionSession<AtomicInteger> session, ExecutionSession.EndReason reason) { closed.incrementAndGet(); }
            public InputResult input(ExecutionSession<AtomicInteger> session, CastInput input) {
                h.assertTrue(CastContexts.current().caster() == actor && CastContexts.current().executor() == executor,
                        "Input retains caster/executor roles inside the same execution scope");
                if (input.hit() == null) return InputResult.REJECTED;
                session.renew(12);
                return received.incrementAndGet() == 2 ? InputResult.COMPLETED : InputResult.ACCEPTED;
            }
        };
        var endpoint = new InputSession<>(h.getLevel(), frame, received, 3,
                InputBinding.held(actor, InteractionHand.MAIN_HAND, recipe), driver, ignored -> {}, () -> true);
        h.assertTrue(InputSessions.start(endpoint), "Register interactive execution");
        h.assertTrue(InputSessions.route(actor, InteractionHand.MAIN_HAND, recipe), "Rejected selection still consumes input");
        h.assertTrue(received.get() == 0, "Rejected input advances no stage");
        h.assertTrue(!InputSessions.route(executor, InteractionHand.MAIN_HAND, recipe), "Different actor cannot route to this endpoint");
        h.runAtTickTime(2, () -> {
            h.assertTrue(InputSessions.route(actor, InteractionHand.MAIN_HAND, recipe, new EntityHitResult(executor)), "Accept new selection");
        });
        h.runAtTickTime(6, () -> {
            h.assertTrue(endpoint.execution().active(), "Accepted input renews the lease beyond its original deadline");
            InputSessions.route(actor, InteractionHand.MAIN_HAND, recipe, new EntityHitResult(executor));
            h.assertTrue(received.get() == 2 && closed.get() == 1 && !InputSessions.waiting(actor), "Completion closes/removes endpoint exactly once");
            h.assertTrue(!InputSessions.route(actor, InteractionHand.MAIN_HAND, recipe), "Ordinary casting resumes after completion");
            actor.discard(); executor.discard(); h.succeed();
        });
    }
    public static void cancellationAndSourceIsolation(GameTestHelper h) {
        var actor = actor(h); var recipe = new Spell(MethodSelf.INSTANCE, EffectHeal.INSTANCE);
        var reasons = new java.util.ArrayList<ExecutionSession.EndReason>();
        java.util.function.Supplier<InputSession<Void>> endpoint = () -> new InputSession<>(h.getLevel(), CastContext.detached(actor, null, null), null,
                20, InputBinding.held(actor, InteractionHand.MAIN_HAND, recipe), new InteractiveDriver<Void>() {
                    public boolean start(ExecutionSession<Void> s) { return true; }
                    public boolean tick(ExecutionSession<Void> s) { return false; }
                    public void close(ExecutionSession<Void> s, ExecutionSession.EndReason r) { reasons.add(r); }
                    public InputResult input(ExecutionSession<Void> s, CastInput i) { throw new AssertionError("Cancelled source cannot execute"); }
                }, ignored -> {}, () -> true);
        InputSessions.start(endpoint.get());
        actor.setShiftKeyDown(true); InputSessions.route(actor, InteractionHand.MAIN_HAND, recipe); actor.setShiftKeyDown(false);
        h.assertTrue(reasons.get(0) == ExecutionSession.EndReason.USER_CANCELLED, "Sneak activation cancels");
        InputSessions.start(endpoint.get()); actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));
        h.assertTrue(!InputSessions.route(actor, InteractionHand.MAIN_HAND, recipe), "A different source retains its normal action");
        h.assertTrue(reasons.get(1) == ExecutionSession.EndReason.SOURCE_CHANGED && !InputSessions.waiting(actor), "Changing source closes stale endpoint");
        InputSessions.start(endpoint.get());
        h.assertTrue(!InputSessions.route(actor, InteractionHand.MAIN_HAND, new Spell(MethodSelf.INSTANCE)), "A different selected recipe starts normally");
        h.assertTrue(reasons.get(2) == ExecutionSession.EndReason.SOURCE_CHANGED, "Recipe changes do not redirect old state");
        actor.discard(); h.succeed();
    }
    public static void continuationResumesOnlyRemainder(GameTestHelper h) {
        var actor = actor(h); var before = new AtomicInteger(); var after = new AtomicInteger();
        ArsContinuation[] remainder = new ArsContinuation[1];
        var prefix = new Probe("prefix", (hit, resolver) -> before.incrementAndGet());
        var pause = new Probe("pause", (hit, resolver) -> {
            remainder[0] = ArsContinuation.capture(resolver); resolver.spellContext.stop();
        });
        var suffix = new Probe("suffix", (hit, resolver) -> {
            h.assertTrue(CastContexts.current().caster() == actor && hit instanceof EntityHitResult, "Resumed context retains roles and new target");
            after.incrementAndGet();
        });
        var resolver = new SpellResolver(new SpellContext(h.getLevel(), new Spell(MethodSelf.INSTANCE, prefix, pause, suffix),
                actor, new LivingCaster(actor), actor.getMainHandItem()));
        resolver.onResolveEffect(h.getLevel(), new EntityHitResult(actor));
        h.assertTrue(before.get() == 1 && after.get() == 0, "Pause holds the remainder");
        remainder[0].resolve(new EntityHitResult(actor)); remainder[0].resolve(new EntityHitResult(actor));
        h.assertTrue(before.get() == 1 && after.get() == 1, "Resume never repeats prefix and is one-shot");
        actor.discard(); h.succeed();
    }
    private static final class Probe extends AbstractEffect {
        private final java.util.function.BiConsumer<HitResult, SpellResolver> action;
        Probe(String id, java.util.function.BiConsumer<HitResult, SpellResolver> action) {
            super(dev.arsconflux.api.projectile.CarrierRegistry.id("conflux_test:" + id), id); this.action = action;
        }
        public boolean isEnabled() { return true; }
        public SpellTier defaultTier() { return SpellTier.ONE; }
        protected java.util.Set<com.hollingsworth.arsnouveau.api.spell.AbstractAugment> getCompatibleAugments() { return java.util.Set.of(); }
        public int getDefaultManaCost() { return 0; }
        public void onResolve(HitResult hit, Level world, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) {
            action.accept(hit, resolver);
        }
    }
}
