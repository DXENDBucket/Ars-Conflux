package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import dev.arsconflux.api.context.*;
import dev.arsconflux.platform.ArsAccess;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraft.core.Direction;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Real optional NEG code paths; reflective lookup keeps absent-addon test/classpaths independent. */
public final class NegTestSupport {
    private NegTestSupport() {}
    private static AbstractSpellPart glyph(String id) {
        return Objects.requireNonNull(GlyphRegistry.getSpellPart(CarrierRegistry.id(id)), id);
    }
    private static LivingEntity cow(GameTestHelper h, int x) {
        var entity = h.spawn(EntityType.COW, x, 3, 2);
        entity.setNoAi(true); entity.setNoGravity(true); entity.setYRot(-90); return entity;
    }
    public static SpellResolver binder(SpellContext context) {
        try {
            return (SpellResolver)Class.forName("alexthw.not_enough_glyphs.api.ThreadwiseSpellResolver")
                    .getConstructor(SpellContext.class).newInstance(context);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    private static SpellContext context(GameTestHelper h, LivingEntity caster, Spell spell) {
        return new SpellContext(h.getLevel(), spell, caster, LivingCaster.from(caster), ItemStack.EMPTY);
    }
    private static ResourceAccount pool(AtomicInteger balance) {
        return new ResourceAccount() {
            public Object reservationKey() { return balance; }
            public boolean canSpend(int amount) { return balance.get() >= amount; }
            public void spend(int amount) { balance.addAndGet(-amount); }
        };
    }
    private record Extension(AtomicInteger derivatives) implements ResolverExtension {
        public ResourceLocation id() { return CarrierRegistry.id("test:source"); }
        public SpellResolver derive(SpellResolver source, SpellResolver proposed) {
            derivatives.incrementAndGet(); return proposed;
        }
    }
    public static void binderDerivationQuotesAndCollisionFocus(GameTestHelper h) {
        var caster = cow(h, 2); var calls = new AtomicInteger();
        var source = binder(context(h, caster, new Spell(MethodSelf.INSTANCE)));
        var extension = new Extension(calls); ResolverContexts.attach(source, extension);
        var derived = source.getNewResolver(context(h, caster, new Spell(MethodSelf.INSTANCE)));
        h.assertTrue(calls.get() == 1 && derived.spellContext.getAttachment(extension.id()) == extension,
                "NEG's overridden factory invokes Core extensions even for a fresh child context");
        h.assertTrue(derived.getClass() == source.getClass(), "Focus-thread resolver is retained");
        var carrier = new EntityProjectileSpell(h.getLevel(), derived);
        carrier.setOwner(caster); carrier.setPos(caster.getEyePosition());
        var trajectory = new ArsTrajectory(new Snowball(h.getLevel(), caster), carrier);
        h.assertTrue(trajectory.start(h.getLevel(), () -> {}, hit -> {}), "Collision wrapper starts");
        var quote = ResolverContexts.quote(ArsAccess.resolver(carrier), derived.spellContext.clone());
        h.assertTrue(quote.getClass() == source.getClass()
                && quote.spellContext.getAttachment(extension.id()) == extension,
                "Native NEG quotes and source extensions survive replacement");
        trajectory.close();

        // Independent custom focus logic must survive the collision-only wrapper as well.
        var focusSource = new SpellResolver(context(h, caster, new Spell(MethodSelf.INSTANCE))) {
            public boolean hasFocus(ItemStack stack) { return stack.is(Items.STICK); }
            public boolean hasFocus(Item item) { return item == Items.STICK; }
        };
        carrier = new EntityProjectileSpell(h.getLevel(), focusSource);
        carrier.setOwner(caster); carrier.setPos(caster.getEyePosition());
        trajectory = new ArsTrajectory(new Snowball(h.getLevel(), caster), carrier);
        h.assertTrue(trajectory.start(h.getLevel(), () -> {}, hit -> {}), "Custom focus wrapper starts");
        h.assertTrue(ArsAccess.resolver(carrier).hasFocus(Items.STICK)
                && ArsAccess.resolver(carrier).hasFocus(Items.STICK.getDefaultInstance()), "Both focus overloads delegate");
        h.assertTrue(!ArsAccess.resolver(carrier).hasFocus(Items.DIRT), "Focus is not broadened by Core");
        trajectory.close(); h.succeed();
    }
    public static void chainFiltersAndPlaneRetainPaidContinuation(GameTestHelper h) {
        var caster = cow(h, 2); var target = h.spawn(EntityType.PIG, 4, 3, 2);
        target.setNoAi(true); target.setNoGravity(true);
        var other = h.spawn(EntityType.PIG, 5, 3, 2); other.setNoAi(true); other.setNoGravity(true);
        var balance = new AtomicInteger(100); var hits = new AtomicInteger();
        var effect = new PaidEffect(caster, balance, hits);
        binder(context(h, caster, new Spell(MethodSelf.INSTANCE, glyph("toomanyglyphs:glyph_chaining"), effect)))
                .onResolveEffect(h.getLevel(), new EntityHitResult(target));
        h.assertTrue(hits.get() >= 2 && balance.get() == 100 - hits.get() * 5,
                "Real NEG chain dispatches and bills each target through the original caster");
        int before = hits.get();
        binder(context(h, caster, new Spell(MethodSelf.INSTANCE, glyph("toomanyglyphs:glyph_filter_player"), effect)))
                .onResolveEffect(h.getLevel(), new EntityHitResult(target));
        h.assertTrue(hits.get() == before, "Failed NEG filter does not reach or pay a provider effect");
        balance.set(15); hits.set(0);
        var pos = h.absolutePos(new net.minecraft.core.BlockPos(3, 3, 3));
        binder(context(h, caster, new Spell(MethodSelf.INSTANCE, glyph("not_enough_glyphs:glyph_propagate_plane"), effect)))
                .onResolveEffect(h.getLevel(), new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, true));
        h.assertTrue(hits.get() == 3 && balance.get() == 0,
                "Real NEG plane attempts multiple positions, but unfunded effects do not execute");
        h.succeed();
    }
    public static void generatedFansUseChildBudgetAndCorrectCarrier(GameTestHelper h) {
        var caster = cow(h, 2); var target = cow(h, 4);
        for (String id : List.of("arsomega:glyph_propagate_projectile", "ars_elemental:glyph_propagator_arc",
                "ars_elemental:glyph_propagator_homing", "arsomega:glyph_propagate_missile")) {
            var action = GlyphRegistry.getSpellPart(CarrierRegistry.id(id));
            if (action == null) continue; // Missile propagator is absent in NEG 1.20.1.
            var balance = new AtomicInteger(17); var profiles = new ArrayList<CarrierProfile>();
            var directions = new ArrayList<Vec3>(); var form = new PaidForm(caster, balance, profiles, directions);
            var spell = new Spell(MethodSelf.INSTANCE, action, AugmentSplit.INSTANCE, AugmentSplit.INSTANCE,
                    AugmentSplit.INSTANCE, AugmentSplit.INSTANCE, form);
            binder(context(h, caster, spell)).onResolveEffect(h.getLevel(), new EntityHitResult(target));
            h.assertTrue(profiles.size() == 3 && balance.get() == 2,
                    "Five requested carriers become three funded carriers for " + id);
            h.assertTrue(profiles.stream().allMatch(p -> p.glyphId().equals(action.getRegistryName())),
                    "Generated carrier identity survives a synthetic recipe for " + id);
            if (id.equals("arsomega:glyph_propagate_projectile")) {
                double low = directions.stream().mapToDouble(v -> v.z).min().orElseThrow();
                double high = directions.stream().mapToDouble(v -> v.z).max().orElseThrow();
                h.assertTrue(low < -.04 && high > .04 && Math.abs(low + high) < .04,
                        "Budget reduction precedes NEG's symmetric fan generation");
            }
            for (var snowball : h.getLevel().getEntitiesOfClass(Snowball.class, caster.getBoundingBox().inflate(16)))
                snowball.discard();
        }
        h.succeed();
    }
    public static void trailAndContingencyUseOriginalSource(GameTestHelper h) {
        var caster = cow(h, 2); var target = cow(h, 4);
        var balance = new AtomicInteger(15); var hits = new AtomicInteger();
        var effect = new PaidEffect(caster, balance, hits);
        try {
            var action = glyph("not_enough_glyphs:glyph_trail");
            var resolver = binder(context(h, caster, new Spell(action, effect)));
            var carrier = (EntityProjectileSpell)Class.forName("alexthw.not_enough_glyphs.common.spell.TrailingProjectile")
                    .getConstructor(net.minecraft.world.level.Level.class, SpellResolver.class)
                    .newInstance(h.getLevel(), resolver);
            carrier.setOwner(caster); carrier.setPos(target.position());
            var trajectory = new ArsTrajectory(new Snowball(h.getLevel(), caster), carrier);
            h.assertTrue(trajectory.start(h.getLevel(), () -> {}, hit -> resolver.onResolveEffect(h.getLevel(), hit)),
                    "Real NEG trail keeps its native carrier with a replacement visual");
            var cast = carrier.getClass().getMethod("castSpells");
            for (int i = 0; i < 5; i++) cast.invoke(carrier);
            h.assertTrue(hits.get() == 3 && balance.get() == 0, "Repeated trail callbacks stop paying at empty balance");
            trajectory.close();

            balance.set(10); hits.set(0);
            var contingency = glyph("not_enough_glyphs:glyph_contingency_fire");
            binder(context(h, caster, new Spell(MethodSelf.INSTANCE, contingency, AugmentSplit.INSTANCE, effect)))
                    .onResolveEffect(h.getLevel(), new EntityHitResult(target));
            var stored = target.getActiveEffects().stream()
                    .filter(e -> e.getClass().getName().endsWith("ContingencyEffectInstance")).findFirst().orElseThrow();
            var trigger = stored.getClass().getMethod("triggerSpell", LivingEntity.class);
            trigger.invoke(stored, target); trigger.invoke(stored, target);
            h.assertTrue(hits.get() == 2 && balance.get() == 0, "Stored contingency retains original payer on both activations");
            h.succeed();
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    private static final class PaidEffect extends AbstractEffect implements TriggerPaidGlyph {
        private final LivingEntity owner; private final AtomicInteger balance, hits;
        PaidEffect(LivingEntity owner, AtomicInteger balance, AtomicInteger hits) {
            super(CarrierRegistry.id("test:glyph_neg_effect"), "NEG fixture");
            this.owner = owner; this.balance = balance; this.hits = hits;
        }
        public int getDefaultManaCost() { return 0; }
        public int getCastingCost() { return 0; }
        public boolean isEnabled() { return true; }
        public SpellTier getConfigTier() { return SpellTier.ONE; }
        public Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
        public void onResolve(HitResult hit, net.minecraft.world.level.Level world, LivingEntity caster,
                SpellStats stats, SpellContext context, SpellResolver resolver) {
            if (context.getUnwrappedCaster() != owner) throw new AssertionError("Continuation changed caster");
            if (!new ResourceCharge(pool(balance), 5).trigger(() -> { hits.incrementAndGet(); return true; })) context.stop();
        }
    }
    private static final class PaidForm extends AbstractAugment implements ProjectileForm {
        private final LivingEntity owner; private final AtomicInteger balance;
        private final List<CarrierProfile> profiles; private final List<Vec3> directions;
        PaidForm(LivingEntity owner, AtomicInteger balance, List<CarrierProfile> profiles, List<Vec3> directions) {
            super(CarrierRegistry.id("test:glyph_neg_payload"), "NEG payload fixture");
            this.owner = owner; this.balance = balance; this.profiles = profiles; this.directions = directions;
        }
        public int getDefaultManaCost() { return 0; }
        public int getCastingCost() { return 0; }
        public boolean isEnabled() { return true; }
        public SpellTier getConfigTier() { return SpellTier.ONE; }
        public int getCostForPart(AbstractSpellPart part) { return 0; }
        public ResourceCharge initialCharge(SpellContext context, List<AbstractAugment> augments) {
            return new ResourceCharge(pool(balance), 5);
        }
        public boolean replace(EntityProjectileSpell projectile, CarrierProfile profile) {
            if (ArsAccess.resolver(projectile).spellContext.getUnwrappedCaster() != owner) throw new AssertionError("Wrong caster");
            initialCharge(ArsAccess.resolver(projectile).spellContext, List.of()).trigger(() -> {
                profiles.add(profile); directions.add(projectile.getDeltaMovement().normalize());
                var visual = new Snowball(projectile.level(), owner); visual.setPos(projectile.position());
                return projectile.level().addFreshEntity(visual);
            });
            return true;
        }
    }
}
