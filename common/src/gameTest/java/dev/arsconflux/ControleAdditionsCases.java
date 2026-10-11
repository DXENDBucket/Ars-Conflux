package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.projectile.ArsTrajectory;
import dev.arsconflux.api.resource.*;
import dev.arsconflux.platform.ArsAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Real optional-addon entrypoints, backed by a provider-neutral execution fixture. */
public final class ControleAdditionsCases {
    private ControleAdditionsCases() {}
    private static Class<?> optional(String name) {
        try { return Class.forName(name); } catch (ClassNotFoundException absent) { return null; }
    }
    private static Object invoke(Object owner, String name, Class<?>[] types, Object... args) {
        try { return (owner instanceof Class<?> type ? type : owner.getClass()).getMethod(name, types)
                .invoke(owner instanceof Class<?> ? null : owner, args); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private static LivingEntity cow(GameTestHelper h, int x) {
        var entity = h.spawn(EntityType.COW, x, 4, 2); entity.setNoAi(true); entity.setNoGravity(true); return entity;
    }
    private static SpellResolver resolver(ServerLevel world, LivingEntity caster, Spell spell) {
        return new SpellResolver(new SpellContext(world, spell, caster, LivingCaster.from(caster), ItemStack.EMPTY));
    }
    private static final class Fixture implements ExecutionDriver<Integer> {
        final LivingEntity caster;
        final ServerLevel origin;
        final SpellResolver source;
        final EntityProjectileSpell carrier;
        final Snowball visual, sibling;
        final ArsTrajectory trajectory;
        final AtomicInteger balance = new AtomicInteger(10), hits = new AtomicInteger();
        final ExecutionSession<Integer> session;
        Level continuationWorld;
        LivingEntity continuationCaster;
        Fixture(GameTestHelper h) {
            origin = h.getLevel(); caster = cow(h, 2);
            var effect = new ProbeEffect((world, owner) -> { continuationWorld = world; continuationCaster = owner; });
            source = resolver(origin, caster, new Spell(MethodProjectile.INSTANCE, effect));
            var account = new ResourceAccount() {
                public Object reservationKey() { return balance; }
                public boolean canSpend(int cost) { return balance.get() >= cost; }
                public void spend(int cost) { balance.addAndGet(-cost); }
            };
            var context = CastContext.of(source.spellContext, null, account);
            CastContexts.bind(source.spellContext, context);
            var frozen = source.clone();
            carrier = new EntityProjectileSpell(origin, source);
            carrier.setPos(caster.position().add(0, 2, 0)); carrier.setDeltaMovement(0.2, 0, 0);
            carrier.age = 7; carrier.pierceLeft = 2; carrier.numSensitive = 1;
            visual = new TrackingSnowball(origin, caster); sibling = new TrackingSnowball(origin, caster);
            trajectory = new ArsTrajectory(visual, carrier);
            session = new ExecutionSession<>(origin, context, 0, 100, this, hit -> {
                var child = frozen.clone(); CastContexts.bind(child.spellContext, CastContexts.current());
                child.onResolveEffect(origin, hit); // Deliberately retain the old closure, as existing providers do.
            }, () -> true, ExecutionScope.DIRECT).billing(TriggerPayment.of(new ResourceCharge(account, 5)), false);
        }
        public boolean start(ExecutionSession<Integer> session) {
            boolean success = trajectory.start(origin, () -> {}, hit -> session.activate(() -> {
                if (visual.level() != carrier.level()) throw new AssertionError("Payload in the wrong world");
                if (CastContexts.current().caster() != caster || CastContexts.current().damageOwner() != caster)
                    throw new AssertionError("Lost damage/source attribution");
                hits.incrementAndGet(); session.impact(hit); return true;
            }));
            sibling.setPos(carrier.position().add(0, 1, 0)); return success && origin.addFreshEntity(sibling);
        }
        public boolean tick(ExecutionSession<Integer> session) { return trajectory.tick(session.world()); }
        public void close(ExecutionSession<Integer> session, ExecutionSession.EndReason reason) { trajectory.close(); sibling.discard(); }
        public Vec3 incomingDirection() { return trajectory.incomingDirection(); }
    }
    /** Tracking assertions are independent of a remote test world's asynchronous chunk visibility. */
    private static final class TrackingSnowball extends Snowball {
        TrackingSnowball(Level world, LivingEntity caster) { super(world, caster); }
        @Override public boolean isAlwaysTicking() { return true; }
    }
    private interface Observer { void accept(Level world, LivingEntity caster); }
    private static final class ProbeEffect extends AbstractEffect {
        private final Observer observer;
        ProbeEffect(Observer observer) { super(dev.arsconflux.api.projectile.CarrierRegistry.id("test:glyph_migrated_hit"), "Migrated hit fixture"); this.observer = observer; }
        public int getDefaultManaCost() { return 0; }
        public boolean isEnabled() { return true; }
        public SpellTier getConfigTier() { return SpellTier.ONE; }
        public java.util.Set<AbstractAugment> getCompatibleAugments() { return java.util.Set.of(); }
        public void onResolve(HitResult hit, Level world, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) {
            observer.accept(world, caster);
        }
    }
    private static EntityProjectileSpell changeLevel(EntityProjectileSpell carrier, ServerLevel destination) {
        var prism = optional("dev.qther.ars_controle.block.WarpingSpellPrismBlock");
        if (prism != null) return (EntityProjectileSpell)invoke(prism, "changeSpellLevel",
                new Class<?>[]{ServerLevel.class, EntityProjectileSpell.class}, destination, carrier);
        if (!ArsTrajectory.redirect(carrier, destination)) throw new AssertionError("Fixture is not managed");
        return carrier;
    }
    private static ServerLevel destination(GameTestHelper h) {
        return h.getLevel().getServer().getLevel(Level.NETHER);
    }
    public static void realPrismResolvesItsDestinationAndKeepsNativeFallback(GameTestHelper h) throws Exception {
        var type = optional("dev.qther.ars_controle.block.WarpingSpellPrismBlock");
        if (type == null) { h.succeed(); return; }
        var fixture = new Fixture(h); var target = destination(h);
        var prism = BuiltInRegistries.BLOCK.get(dev.arsconflux.api.projectile.CarrierRegistry.id("ars_controle:warping_spell_prism"));
        h.assertTrue(type.isInstance(prism), "Installed native prism is registered");
        var prismPos = h.absolutePos(new BlockPos(4, 4, 4));
        var targetPos = new BlockPos(prismPos.getX(), 32, prismPos.getZ());
        target.getChunkAt(targetPos); var previousBlock = target.getBlockState(targetPos);
        var configType = optional("dev.qther.ars_controle.config.ACServerConfig");
        if (configType == null) configType = Class.forName("dev.qther.ars_controle.ServerConfig");
        var config = configType.getField("SERVER").get(null);
        var cost = configType.getField("WARPING_SPELL_PRISM_COST_DIMENSION").get(config);
        Object previousCost = invoke(cost, "get", new Class<?>[]{});
        Object loadTime = null;
        try { loadTime = configType.getField("WARPING_SPELL_PRISM_LOAD_TIME").get(config); }
        catch (NoSuchFieldException notInForge) {}
        final Object loading = loadTime;
        final Object previousLoading = loading == null ? null : invoke(loading, "get", new Class<?>[]{});
        long chunk = net.minecraft.world.level.ChunkPos.asLong(targetPos.getX() >> 4, targetPos.getZ() >> 4);
        boolean forced = target.getForcedChunks().contains(chunk);
        target.setChunkForced(targetPos.getX() >> 4, targetPos.getZ() >> 4, true);
        // Forge's prism explicitly rejects unloaded destinations; let the remote chunk become visible.
        h.runAfterDelay(5, () -> {
            try {
                h.assertTrue(target.isLoaded(targetPos), "Native prism target chunk is loaded");
                invoke(cost, "set", new Class<?>[]{Object.class}, 0);
                if (loading != null) invoke(loading, "set", new Class<?>[]{Object.class}, 0);
                target.setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);
                fixture.origin.setBlock(prismPos, prism.defaultBlockState(), 3);
                var tile = fixture.origin.getBlockEntity(prismPos);
                invoke(tile, "setBlock", new Class<?>[]{net.minecraft.resources.ResourceKey.class, BlockPos.class}, target.dimension(), targetPos);
                h.assertTrue(invoke(tile, "getTargetLevel", new Class<?>[]{}) == target, "Native target world selected");
                h.assertTrue(ExecutionSessions.start(fixture.session), "Prism fixture starts");
                fixture.session.activate(() -> {
                    invoke(prism, "onHit", new Class<?>[]{ServerLevel.class, BlockPos.class, EntityProjectileSpell.class},
                            fixture.origin, prismPos, fixture.carrier);
                    return true;
                });
                h.assertTrue(fixture.hits.get() == 1 && fixture.continuationWorld == target && fixture.carrier.isRemoved(),
                        "Actual native prism selects, teleports and resolves the managed hit: hits=" + fixture.hits.get()
                        + ", world=" + fixture.carrier.level().dimension() + ", removed=" + fixture.carrier.isRemoved());
                // Unmanaged Ars projectiles retain Controle's original clone-and-remove path.
                var plain = new EntityProjectileSpell(fixture.origin, resolver(fixture.origin, fixture.caster, new Spell(MethodProjectile.INSTANCE)));
                plain.setPos(targetPos.getCenter());
                var moved = changeLevel(plain, target);
                h.assertTrue(moved != plain && plain.isRemoved() && moved.level() == target, "Plain Ars preserves the native dimension transfer");
                moved.discard();
            } finally {
                fixture.session.finish(ExecutionSession.EndReason.COMPLETED);
                fixture.origin.removeBlock(prismPos, false); target.setBlock(targetPos, previousBlock, 3);
                invoke(cost, "set", new Class<?>[]{Object.class}, previousCost);
                if (loading != null) invoke(loading, "set", new Class<?>[]{Object.class}, previousLoading);
                if (!forced) target.setChunkForced(targetPos.getX() >> 4, targetPos.getZ() >> 4, false);
            }
            h.succeed();
        });
    }

    public static void transferKeepsInstancesSourcePaymentAndContinuation(GameTestHelper h) {
        var fixture = new Fixture(h);
        var target = destination(h);
        var position = fixture.carrier.position().add(0, 20, 0);
        target.getChunkAt(BlockPos.containing(position));
        try {
            h.assertTrue(ExecutionSessions.start(fixture.session), "Execution starts and funds once");
            h.assertTrue(changeLevel(fixture.carrier, target) == fixture.carrier, "Prism retains the managed carrier instance");
            h.assertTrue(fixture.carrier.age == 7 && fixture.carrier.pierceLeft == 2 && fixture.carrier.numSensitive == 1,
                    "Native carrier state survives the dimension change");
            var hit = new BlockHitResult(position, Direction.UP, BlockPos.containing(position), false);
            fixture.session.activate(() -> { ArsAccess.resolver(fixture.carrier).onResolveEffect(target, hit); return true; });
            h.assertTrue(fixture.visual.level() == target && fixture.sibling.level() == target,
                    "All managed visuals move before payload dispatch");
            h.assertTrue(target.getEntity(fixture.visual.getUUID()) == fixture.visual && target.getEntity(fixture.sibling.getUUID()) == fixture.sibling,
                    "Destination tracks the same instances and UUIDs");
            h.assertTrue(fixture.origin.getEntity(fixture.visual.getUUID()) == null, "Origin stops tracking the visual");
            h.assertTrue(target.getEntity(fixture.carrier.getUUID()) == null, "The off-world Ars carrier is not duplicated into a free projectile");
            h.assertTrue(fixture.hits.get() == 1 && fixture.balance.get() == 5, "Impact pays no extra launch charge");
            h.assertTrue(fixture.continuationWorld == target && fixture.continuationCaster == fixture.caster,
                    "Existing frozen continuations resolve in the destination with the original caster");
            h.assertTrue(fixture.session.permitted() && fixture.session.remainingTicks() == 100, "Remote trajectory retains origin owner and lifetime");
            // An unrelated cast by the same caster must not be relocated by the active execution scope.
            var unrelated = resolver(fixture.origin, fixture.caster, new Spell(MethodProjectile.INSTANCE,
                    new ProbeEffect((world, caster) -> h.assertTrue(world == fixture.origin, "Independent cast keeps its world"))));
            fixture.session.activate(() -> { unrelated.onResolveEffect(fixture.origin, hit); return true; });
            ExecutionSessions.unload(target);
            h.assertTrue(!fixture.session.active() && fixture.visual.isRemoved() && fixture.sibling.isRemoved(), "Destination unload closes the execution");
        } finally { fixture.session.finish(ExecutionSession.EndReason.COMPLETED); }
        h.succeed();
    }
    public static void transferFailureAndOwnerDepartureCloseCleanly(GameTestHelper h) {
        var fixture = new Fixture(h); var target = destination(h);
        var duplicate = new Snowball(target, fixture.caster); duplicate.setUUID(fixture.visual.getUUID());
        duplicate.setPos(fixture.carrier.position()); target.getChunkAt(duplicate.blockPosition());
        try {
            h.assertTrue(target.addFreshEntity(duplicate), "Collision fixture spawned");
            h.assertTrue(ExecutionSessions.start(fixture.session), "Source execution starts");
            changeLevel(fixture.carrier, target);
            fixture.session.activate(() -> { ArsAccess.resolver(fixture.carrier).onResolveEffect(target,
                    new BlockHitResult(duplicate.position(), Direction.UP, duplicate.blockPosition(), false)); return true; });
            h.assertTrue(!fixture.session.active() && fixture.hits.get() == 0 && fixture.visual.isRemoved() && fixture.sibling.isRemoved(),
                    "Rejected destination spawn stops payload and all sibling visuals");
        } finally { duplicate.discard(); fixture.session.finish(ExecutionSession.EndReason.COMPLETED); }
        var departed = new Fixture(h);
        try {
            h.assertTrue(ExecutionSessions.start(departed.session), "Second source starts");
            changeLevel(departed.carrier, target); departed.carrier.setPos(departed.carrier.position().add(0, 20, 0));
            departed.session.activate(() -> { ArsAccess.resolver(departed.carrier).onResolveEffect(target,
                    new BlockHitResult(departed.carrier.position(), Direction.UP, departed.carrier.blockPosition(), false)); return true; });
            departed.caster.discard(); departed.session.tick();
            h.assertTrue(!departed.session.active() && departed.visual.isRemoved(), "Caster departure still cancels the remote execution");
        } finally { departed.session.finish(ExecutionSession.EndReason.COMPLETED); }
        h.succeed();
    }
    private static final class DamageProbe extends Cow {
        Runnable callback = () -> {};
        DamageProbe(Level world) { super(EntityType.COW, world); setNoAi(true); setNoGravity(true); }
        @Override protected void actuallyHurt(DamageSource source, float amount) {
            super.actuallyHurt(source, amount); callback.run();
        }
    }
    public static void retaliationUsesCurrentDamageAndRestoresNestedScopes(GameTestHelper h) {
        var type = optional("com.github.jarva.arsadditions.common.glyph.MethodRetaliate");
        var victim = new DamageProbe(h.getLevel()); victim.setPos(h.absoluteVec(new Vec3(3, 4, 3))); h.getLevel().addFreshEntity(victim);
        var first = cow(h, 6); var second = cow(h, 8);
        try {
            Object retaliate = type == null ? null : type.getField("INSTANCE").get(null);
            LivingEntity[] selected = {null};
            var source = resolver(h.getLevel(), victim, new Spell(MethodProjectile.INSTANCE));
            var capture = new SpellResolver(source.spellContext) {
                @Override public void onResolveEffect(Level level, HitResult hit) { selected[0] = (LivingEntity)((EntityHitResult)hit).getEntity(); }
            };
            victim.callback = () -> {
                selected[0] = null;
                if (retaliate != null) invoke(retaliate, "cast", new Class<?>[]{LivingEntity.class, SpellResolver.class}, victim, capture);
                else selected[0] = RetaliationTargets.currentAttacker(victim);
            };
            victim.hurt(victim.damageSources().mobAttack(first), 1);
            h.assertTrue(selected[0] == first, "First reactive hit targets its actual attacker");
            victim.invulnerableTime = 0; victim.hurt(victim.damageSources().mobAttack(second), 1);
            h.assertTrue(selected[0] == second, "Next reactive hit does not target the previous attacker");
            victim.invulnerableTime = 0; victim.hurt(victim.damageSources().magic(), 1);
            h.assertTrue(selected[0] == null, "Unattributed damage does not retaliate against an old attacker");
            h.assertTrue(!RetaliationTargets.isTakingDamage(victim), "Real hurt callback scope is removed");
            RetaliationTargets.during(victim, victim.damageSources().mobAttack(first), () -> {
                try { RetaliationTargets.during(victim, victim.damageSources().mobAttack(second), () -> { throw new IllegalStateException("probe"); }); }
                catch (IllegalStateException expected) {}
                h.assertTrue(RetaliationTargets.currentAttacker(victim) == first, "Nested exception restores parent damage source");
                return null;
            });
            h.assertTrue(!RetaliationTargets.isTakingDamage(victim), "Nested callback leaves no stale scope");
            if (retaliate != null) {
                selected[0] = null; invoke(retaliate, "cast", new Class<?>[]{LivingEntity.class, SpellResolver.class}, victim, capture);
                h.assertTrue(selected[0] == second, "Manual retaliate outside damage keeps Additions' damage history");
            }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        finally { victim.discard(); }
        h.succeed();
    }
}
