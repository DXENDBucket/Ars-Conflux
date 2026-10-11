package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.*;
import dev.arsconflux.platform.ArsAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual optional-addon paths, with provider fixtures rather than a dependency on Iron or Goety. */
public final class ElementalTestSupport {
    private ElementalTestSupport() {}
    private record Origin(UUID owner) implements ResolverExtension {
        public ResourceLocation id() { return CarrierRegistry.id("test:elemental_origin"); }
    }
    private static AbstractSpellPart glyph(String id) {
        return Objects.requireNonNull(GlyphRegistry.getSpellPart(CarrierRegistry.id(id)), id);
    }
    private static LivingEntity caster(GameTestHelper h) {
        var caster = h.spawn(EntityType.COW, 2, 4, 2);
        caster.setNoAi(true); caster.setNoGravity(true); caster.setYRot(-90); return caster;
    }
    private static SpellResolver resolver(GameTestHelper h, LivingEntity caster, Spell spell) {
        var resolver = new SpellResolver(new SpellContext(h.getLevel(), spell, caster,
                LivingCaster.from(caster), ItemStack.EMPTY));
        ResolverContexts.attach(resolver, new Origin(caster.getUUID()));
        return resolver;
    }
    private static ResourceAccount pool(AtomicInteger balance) {
        return new ResourceAccount() {
            public Object reservationKey() { return balance; }
            public boolean canSpend(int amount) { return balance.get() >= amount; }
            public void spend(int amount) { balance.addAndGet(-amount); }
        };
    }
    public static void generatedFansRetainElementalIdentityAndBudget(GameTestHelper h) {
        var caster = caster(h);
        for (String id : List.of("ars_elemental:glyph_propagator_arc", "ars_elemental:glyph_propagator_homing")) {
            var action = glyph(id);
            h.assertTrue(action.getClass().getName().startsWith("alexthw.ars_elemental."),
                    "Elemental owns this glyph even with NEG installed");
            var paid = new Payload(caster, new AtomicInteger(17));
            var source = resolver(h, caster, new Spell(MethodSelf.INSTANCE, action,
                    AugmentSplit.INSTANCE, AugmentSplit.INSTANCE, AugmentSplit.INSTANCE, AugmentSplit.INSTANCE, paid));
            source.onResolveEffect(h.getLevel(), new EntityHitResult(caster));
            h.assertTrue(paid.carriers.size() == 3 && paid.balance.get() == 2,
                    "Elemental's five requested branches become three funded branches for " + id);
            h.assertTrue(paid.profiles.stream().allMatch(p -> p.glyphId().equals(action.getRegistryName())),
                    "Child emission retains its actual form for " + id);
            paid.close();

            var empty = new Payload(caster, new AtomicInteger(0));
            resolver(h, caster, new Spell(MethodSelf.INSTANCE, action, AugmentSplit.INSTANCE, empty))
                    .onResolveEffect(h.getLevel(), new EntityHitResult(caster));
            h.assertTrue(empty.carriers.isEmpty() && empty.balance.get() == 0, "Empty accounts cannot launch a replacement");
            h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, caster.getBoundingBox().inflate(16)).isEmpty(),
                    "An unfunded payload cannot fall back to a free native carrier");

            var plain = resolver(h, caster, new Spell(MethodSelf.INSTANCE, action,
                    AugmentSplit.INSTANCE, AugmentSplit.INSTANCE));
            plain.onResolveEffect(h.getLevel(), new EntityHitResult(caster));
            var nativeEntities = h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, caster.getBoundingBox().inflate(16));
            h.assertTrue(nativeEntities.size() == 3, "Plain Elemental keeps its original fan");
            nativeEntities.forEach(Entity::discard);
        }
        h.succeed();
    }
    public static void phalanxKeepsFormationLaunchHitsAndCleanup(GameTestHelper h) {
        var action = GlyphRegistry.getSpellPart(CarrierRegistry.id("ars_elemental:glyph_carian_phalanx"));
        if (action == null) { h.succeed(); return; } // Not present in Elemental 1.20.1.
        var caster = caster(h);
        var paid = new Payload(caster, new AtomicInteger(17));
        var hits = new AtomicInteger();
        var effect = new Continuation(caster, hits);
        var source = resolver(h, caster, new Spell(action, AugmentSplit.INSTANCE, AugmentSplit.INSTANCE,
                AugmentSplit.INSTANCE, AugmentSplit.INSTANCE, paid, effect));
        ((AbstractCastMethod)action).onCast(ItemStack.EMPTY, caster, h.getLevel(), source.getCastStats(), source.spellContext, source);
        h.assertTrue(paid.carriers.size() == 3 && paid.balance.get() == 2, "Phalanx is budgeted before it allocates formation slots");
        h.assertTrue(paid.profiles.stream().allMatch(p -> p.glyphId().equals(action.getRegistryName()) && p.arsDriven() && !p.repeated()),
                "Phalanx is a native driven, once-paid carrier");
        h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, caster.getBoundingBox().inflate(16)).isEmpty(),
                "Only replacement visuals enter the world");
        for (int tick = 0; tick < 8; tick++) paid.tick(h);
        h.assertTrue(paid.carriers.stream().allMatch(p -> !launched(p)), "Phalanxes wait in their formation before finding enemies");
        h.assertTrue(paid.carriers.stream().map(Entity::position).distinct().count() == 3, "Native formation has distinct slots");
        var target = h.spawn(EntityType.ZOMBIE, 8, 4, 2);
        target.setNoAi(true); target.setNoGravity(true);
        boolean launched = false;
        for (int tick = 0; tick < 180 && hits.get() < 3; tick++) {
            paid.tick(h);
            launched |= paid.carriers.stream().anyMatch(ElementalTestSupport::launched);
        }
        h.assertTrue(launched, "Native phalanx detects an enemy and leaves formation");
        h.assertTrue(hits.get() == 3 && paid.balance.get() == 2, "All three homing carriers hit and keep once-only launch payment");
        paid.close(); target.discard();
        h.assertTrue(paid.carriers.stream().allMatch(Entity::isRemoved), "Closing replacement trajectories removes their native carriers");

        // Native cleanup must free dead formation slots on the next use.
        var next = new Payload(caster, new AtomicInteger(5));
        source = resolver(h, caster, new Spell(action, next));
        ((AbstractCastMethod)action).onCast(ItemStack.EMPTY, caster, h.getLevel(), source.getCastStats(), source.spellContext, source);
        h.assertTrue(next.carriers.size() == 1 && next.balance.get() == 0, "Recasting replaces stale native formation entries");
        next.close();
        var empty = new Payload(caster, new AtomicInteger(0));
        source = resolver(h, caster, new Spell(action, AugmentSplit.INSTANCE, empty));
        ((AbstractCastMethod)action).onCast(ItemStack.EMPTY, caster, h.getLevel(), source.getCastStats(), source.spellContext, source);
        h.assertTrue(empty.carriers.isEmpty(), "An empty phalanx account creates no formation slots");
        h.succeed();
    }
    private static boolean launched(EntityProjectileSpell carrier) {
        try { return (boolean)carrier.getClass().getMethod("isLaunched").invoke(carrier); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    private static final class Continuation extends AbstractEffect implements TriggerPaidGlyph {
        private final LivingEntity owner; private final AtomicInteger hits;
        Continuation(LivingEntity owner, AtomicInteger hits) {
            super(CarrierRegistry.id("test:glyph_elemental_hit"), "Elemental continuation fixture");
            this.owner = owner; this.hits = hits;
        }
        public int getDefaultManaCost() { return 0; }
        public int getCastingCost() { return 0; }
        public boolean isEnabled() { return true; }
        public SpellTier getConfigTier() { return SpellTier.ONE; }
        public Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
        public void onResolve(HitResult hit, Level world, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) {
            if (caster != owner || context.getUnwrappedCaster() != owner) throw new AssertionError("Phalanx changed continuation owner");
            if (hit instanceof EntityHitResult result && result.getEntity().getType() == EntityType.ZOMBIE) hits.incrementAndGet();
        }
    }
    private static final class Payload extends AbstractAugment implements ProjectileForm {
        private final LivingEntity owner; private final AtomicInteger balance;
        private final List<EntityProjectileSpell> carriers = new ArrayList<>();
        private final List<CarrierProfile> profiles = new ArrayList<>();
        private final List<ArsTrajectory> trajectories = new ArrayList<>();
        Payload(LivingEntity owner, AtomicInteger balance) {
            super(CarrierRegistry.id("test:glyph_elemental_payload"), "Elemental payload fixture");
            this.owner = owner; this.balance = balance;
        }
        public int getDefaultManaCost() { return 0; }
        public int getCastingCost() { return 0; }
        public boolean isEnabled() { return true; }
        public SpellTier getConfigTier() { return SpellTier.ONE; }
        public int getCostForPart(AbstractSpellPart part) { return 0; }
        public ResourceCharge initialCharge(SpellContext context, List<AbstractAugment> augments) { return new ResourceCharge(pool(balance), 5); }
        public boolean replace(EntityProjectileSpell carrier, CarrierProfile profile) {
            var original = ArsAccess.resolver(carrier);
            var origin = (Origin)original.spellContext.getAttachment(CarrierRegistry.id("test:elemental_origin"));
            if (origin == null || !origin.owner().equals(owner.getUUID())) throw new AssertionError("Lost source extension");
            initialCharge(original.spellContext, List.of()).trigger(() -> {
                var trajectory = new ArsTrajectory(new Snowball(carrier.level(), owner), carrier);
                if (!trajectory.start((net.minecraft.server.level.ServerLevel)carrier.level(), () -> {},
                        hit -> original.onResolveEffect(carrier.level(), hit))) return false;
                carriers.add(carrier); profiles.add(profile); trajectories.add(trajectory); return true;
            });
            return true;
        }
        void tick(GameTestHelper h) { trajectories.forEach(t -> t.tick(h.getLevel())); }
        void close() { trajectories.forEach(ArsTrajectory::close); }
    }
}
