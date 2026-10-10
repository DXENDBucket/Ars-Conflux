package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.glyph.SpellValidators;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Deliberately runs with Ars and its dependencies only, without Iron, Goety or glyph addons. */
@GameTestHolder("ars_conflux") @PrefixGameTestTemplate(false)
public final class ConfluxGameTests {
    private static LivingEntity cow(GameTestHelper h, int x) {
        var caster = h.spawn(EntityType.COW, x, 3, 2);
        caster.setNoAi(true); caster.setNoGravity(true); caster.setYRot(-90); return caster;
    }
    @GameTest(template="empty", timeoutTicks=30)
    public static void twoIndependentProvidersUseOneSpawnHookAndVanillaArsPassesThrough(GameTestHelper h) {
        h.assertTrue(!ModList.get().isLoaded("irons_nouveau") && !ModList.get().isLoaded("irons_spellbooks")
                && !ModList.get().isLoaded("not_enough_glyphs"), "Core runs without external spell content or optional glyph addons");
        var caster = cow(h, 2); var balance = new AtomicInteger(1000);
        var callsA = new AtomicInteger(); var callsB = new AtomicInteger();
        var a = new TestForm("provider_a:glyph_payload", callsA);
        var b = new TestForm("provider_b:glyph_payload", callsB);
        MethodProjectile.INSTANCE.compatibleAugments.add(a); MethodProjectile.INSTANCE.compatibleAugments.add(b);
        for (var form : List.of(a, b)) {
            var source = new LivingCaster(caster) {
                public boolean enoughMana(int amount) { return balance.get() >= amount; }
                public void expendMana(int amount) { balance.addAndGet(-amount); }
            };
            var resolver = new SpellResolver(new SpellContext(h.getLevel(), new Spell(MethodProjectile.INSTANCE, form), caster, source, ItemStack.EMPTY));
            int before = balance.get();
            h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "Provider cast dispatches through real Ars spawn");
            h.assertTrue(before - balance.get() == resolver.getExpendedCost() + 5, "Ars upfront and provider trigger paid exactly once");
        }
        h.assertTrue(callsA.get() == 1 && callsB.get() == 1, "Each payload reaches its own provider without consulting its peer");
        h.assertTrue(h.getLevel().getEntitiesOfClass(Snowball.class, caster.getBoundingBox().inflate(16)).size() == 2, "Two replacement entities");
        h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, caster.getBoundingBox().inflate(16)).isEmpty(), "No duplicate original carriers");
        var plainSource = new LivingCaster(caster) {
            public boolean enoughMana(int amount) { return balance.get() >= amount; }
            public void expendMana(int amount) { balance.addAndGet(-amount); }
        };
        var plain = new SpellResolver(new SpellContext(h.getLevel(), new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE), caster, plainSource, ItemStack.EMPTY));
        h.assertTrue(plain.onCast(ItemStack.EMPTY, h.getLevel()), "Ordinary Ars still casts");
        h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, caster.getBoundingBox().inflate(16)).size() == 1, "Unmanaged Ars carrier remains untouched");
        h.assertTrue(!SpellValidators.validate(List.of(MethodProjectile.INSTANCE, a, b)).isEmpty(), "Mixed providers cannot claim the same carrier");
        MethodProjectile.INSTANCE.compatibleAugments.remove(a); MethodProjectile.INSTANCE.compatibleAugments.remove(b);
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=30)
    public static void foreignVisualBorrowsArsMotionAndCollisionWithoutIron(GameTestHelper h) {
        var caster = cow(h, 2); var target = cow(h, 7); var hits = new AtomicInteger();
        var resolver = new SpellResolver(new SpellContext(h.getLevel(), new Spell(MethodProjectile.INSTANCE), caster, LivingCaster.from(caster), ItemStack.EMPTY));
        var carrier = new EntityProjectileSpell(h.getLevel(), resolver);
        carrier.setOwner(caster); carrier.setPos(caster.getEyePosition());
        carrier.setDeltaMovement(target.getBoundingBox().getCenter().subtract(carrier.position()).normalize().scale(.75));
        var visual = new Snowball(h.getLevel(), caster);
        var trajectory = new ArsTrajectory(visual, carrier);
        h.assertTrue(trajectory.start(h.getLevel(), () -> {}, hit -> {
            if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() == target) hits.incrementAndGet();
        }), "A foreign visual spawns through common trajectory driver");
        var origin = visual.position();
        for (int i = 0; i < 12 && hits.get() == 0; i++) trajectory.tick(h.getLevel());
        h.assertTrue(visual.position().distanceTo(origin) > 1, "Common driver updates foreign entity position");
        h.assertTrue(hits.get() == 1, "Original Ars collision reports to provider callback");
        trajectory.close();
        h.assertTrue(visual.isRemoved() && carrier.isRemoved(), "Both entities cleaned by common driver");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=30)
    public static void sharedLifecycleOwnsOccupancyExpirationAndFailedSpawnCleanup(GameTestHelper h) {
        var caster = cow(h, 2); var closed = new AtomicInteger(); var continuationClosed = new AtomicInteger();
        var session = new ExecutionSession<>(h.getLevel(), caster, "foreign-parameters", 3, new ExecutionDriver<String>() {
            public boolean start(ExecutionSession<String> session) { return true; }
            public boolean tick(ExecutionSession<String> session) { return false; }
            public boolean occupiesCaster() { return true; }
            public void close(ExecutionSession<String> session, ExecutionSession.EndReason reason) { closed.incrementAndGet(); }
        }, new ImpactContinuation() {
            public void resolve(HitResult hit) {}
            public void close() { continuationClosed.incrementAndGet(); }
        }, () -> true, ExecutionScope.DIRECT);
        h.assertTrue(ExecutionSessions.start(session) && ExecutionSessions.isCasting(caster), "Occupancy uses declared capability, not an Iron class");
        var failed = new ExecutionSession<>(h.getLevel(), caster, "different-parameters", 10, new ExecutionDriver<String>() {
            public boolean start(ExecutionSession<String> session) { return false; }
            public boolean tick(ExecutionSession<String> session) { throw new AssertionError("Failed spawn must not tick"); }
            public void close(ExecutionSession<String> session, ExecutionSession.EndReason reason) { closed.incrementAndGet(); }
        }, hit -> {}, () -> true, ExecutionScope.DIRECT);
        h.assertTrue(!ExecutionSessions.start(failed), "Failed driver is rejected");
        h.assertTrue(closed.get() == 1 && failed.state() == ExecutionSession.State.CANCELLED, "Failed spawn cleaned once");
        h.runAtTickTime(5, () -> {
            h.assertTrue(!session.active() && !ExecutionSessions.isCasting(caster), "Expiry releases occupancy through Core tick hook");
            session.finish(ExecutionSession.EndReason.SERVER_STOPPED);
            h.assertTrue(closed.get() == 2 && continuationClosed.get() == 1, "Repeated cleanup remains idempotent");
            h.succeed();
        });
    }
    @GameTest(template="empty", timeoutTicks=30)
    public static void cleanupFailureStillClosesContinuationAndDirectionSurvivesContextClone(GameTestHelper h) {
        var caster = cow(h, 2); var executor = cow(h, 5); var closed = new AtomicInteger();
        var ars = new SpellContext(h.getLevel(), new Spell(MethodProjectile.INSTANCE), caster, LivingCaster.from(caster), ItemStack.EMPTY);
        var direction = new Vec3(1, -1, 0).normalize(); TriggerGeometry.write(ars, direction);
        var clone = ars.clone();
        var context = CastContext.of(clone, new EntityHitResult(executor), new ArsManaAccount(clone.getCaster())).withExecutor(executor);
        h.assertTrue(context.executor() == executor && context.caster() == caster && context.damageOwner() == caster,
                "Proxy execution preserves original owner and resource source");
        h.assertTrue(context.incomingDirection().dot(direction) > .9999, "Deferred/cloned context preserves incoming direction");
        var session = new ExecutionSession<>(h.getLevel(), caster, 0, 10, new ExecutionDriver<Integer>() {
            public boolean start(ExecutionSession<Integer> session) { return true; }
            public boolean tick(ExecutionSession<Integer> session) { return false; }
            public void close(ExecutionSession<Integer> session, ExecutionSession.EndReason reason) { throw new IllegalStateException("Fixture cleanup failure"); }
        }, new ImpactContinuation() {
            public void resolve(HitResult hit) {}
            public void close() { closed.incrementAndGet(); }
        }, () -> true, ExecutionScope.DIRECT);
        session.start();
        try { session.finish(ExecutionSession.EndReason.COMPLETED); throw new AssertionError("Expected cleanup exception"); }
        catch (IllegalStateException expected) { h.assertTrue(closed.get() == 1 && !session.active(), "Continuation cleanup runs despite driver failure"); }
        h.succeed();
    }
    private static final class TestForm extends AbstractAugment implements ProjectileForm {
        private final AtomicInteger calls;
        TestForm(String id, AtomicInteger calls) { super(CarrierRegistry.id(id), id); this.calls = calls; }
        public int getDefaultManaCost() { return 0; }
        public int getCastingCost() { return 0; }
        public boolean isEnabled() { return true; }
        public SpellTier getConfigTier() { return SpellTier.ONE; }
        public int getCostForPart(AbstractSpellPart part) { return 0; }
        public ResourceCharge initialCharge(SpellContext context, List<AbstractAugment> augments) {
            return new ResourceCharge(new ArsManaAccount(context.getCaster()), 5);
        }
        public boolean replace(EntityProjectileSpell projectile, CarrierProfile carrier) {
            var context = projectile.resolver().spellContext;
            initialCharge(context, List.of()).trigger(() -> {
                var replacement = new Snowball(projectile.level(), context.getUnwrappedCaster());
                replacement.setPos(projectile.position()); replacement.setDeltaMovement(projectile.getDeltaMovement());
                boolean spawned = projectile.level().addFreshEntity(replacement);
                if (spawned) calls.incrementAndGet(); return spawned;
            });
            return true;
        }
    }
}
