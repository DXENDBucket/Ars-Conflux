package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import dev.arsconflux.api.casting.ActiveCastPolicy;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.glyph.AugmentEquivalence;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Exercises installed AAP classes. Optional providers are inspected without compile-time dependencies. */
public final class AdamsCompatibilityCases {
    private static final ResourceLocation SOURCE = CarrierRegistry.id("ars_conflux:test_adams_source");
    private static AbstractSpellPart part(String simpleName) {
        return GlyphRegistry.getSpellpartMap().values().stream()
                .filter(p -> p.getClass().getSimpleName().equals(simpleName)
                        && p.getClass().getName().contains("adamsarsplus"))
                .findFirst().orElseThrow(() -> new AssertionError("AAP glyph missing: " + simpleName));
    }
    private static AbstractAugment augment(String name) { return (AbstractAugment)part("Augment" + name); }
    private static LivingEntity cow(GameTestHelper h, int x) {
        var entity = h.spawn(EntityType.COW, x, 3, 2);
        entity.setNoAi(true); entity.setNoGravity(true); return entity;
    }
    public static void absent(GameTestHelper h) {
        h.assertTrue(GlyphRegistry.getSpellpartMap().values().stream()
                .noneMatch(part -> part.getClass().getName().contains("adamsarsplus")), "AAP is absent in this run");
        var profile = CarrierRegistry.get(CarrierRegistry.id("ars_nouveau:glyph_projectile"));
        h.assertTrue(AugmentEquivalence.sum(List.of(AugmentAmplify.INSTANCE, AugmentAmplify.INSTANCE), AugmentAmplify.INSTANCE) == 2
                        && profile.augments().contains(AugmentAccelerate.INSTANCE),
                "Standalone native augments and carrier support remain available");
        var caster = cow(h, 2);
        var resolver = new SpellResolver(new SpellContext(h.getLevel(), new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE),
                caster, new LivingCaster(caster) {
                    public boolean enoughMana(int amount) { return true; }
                    public void expendMana(int amount) {}
                }, ItemStack.EMPTY));
        h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "Native Ars still casts with no AAP or providers installed");
        h.succeed();
    }
    public static void augments(GameTestHelper h) {
        var bases = Map.of("Amplify", AugmentAmplify.INSTANCE, "Dampen", AugmentDampen.INSTANCE,
                "AOE", AugmentAOE.INSTANCE, "Accelerate", AugmentAccelerate.INSTANCE,
                "ExtendTime", AugmentExtendTime.INSTANCE, "DurationDown", AugmentDurationDown.INSTANCE);
        var profile = CarrierRegistry.get(CarrierRegistry.id("ars_nouveau:glyph_projectile"));
        for (var entry : bases.entrySet()) {
            var two = augment(entry.getKey() + "Two"); var three = augment(entry.getKey() + "Three");
            h.assertTrue(AugmentEquivalence.sum(List.of(entry.getValue(), two, three), entry.getValue()) == 7,
                    entry.getKey() + " equivalents retain 1/2/4 units");
            h.assertTrue(profile.augments().contains(two) == profile.augments().contains(entry.getValue()),
                    "Carrier eligibility follows the base augment without adding unsupported families");
        }
        var stats = augment("AmplifyThree").applyModifiers(new SpellStats.Builder(), EffectHarm.INSTANCE).build();
        h.assertTrue(stats.getAmpMultiplier() == 4, "Native SpellStats receives four units, without a second application");
        h.assertTrue(AugmentEquivalence.units(augment("LesserAOE"), AugmentAOE.INSTANCE) == -1,
                "Reduced AOE retains its negative contribution");
        h.succeed();
    }
    private static final class Account implements ResourceAccount {
        int balance = 14;
        public Object reservationKey() { return this; }
        public boolean canSpend(int amount) { return balance >= amount; }
        public void spend(int amount) { balance -= amount; }
    }
    private record Source(AtomicInteger resolutions) implements ResolverExtension {
        public ResourceLocation id() { return SOURCE; }
        public SpellResolver derive(SpellResolver source, SpellResolver proposed) {
            return proposed.getClass() == SpellResolver.class ? new TrackedResolver(proposed.spellContext, resolutions) : proposed;
        }
    }
    private static final class TrackedResolver extends SpellResolver {
        final AtomicInteger resolutions;
        TrackedResolver(SpellContext context, AtomicInteger resolutions) { super(context); this.resolutions = resolutions; }
        @Override public void onResolveEffect(Level world, HitResult hit) {
            resolutions.incrementAndGet(); super.onResolveEffect(world, hit);
        }
    }
    private static final class Probe extends AbstractEffect implements TriggerPaidGlyph {
        final Account account; int attempts; int successes; CastContext last; ActiveCastPolicy policy;
        Probe(Account account) { super(CarrierRegistry.id("ars_conflux:glyph_adams_probe"), "Probe"); this.account = account; }
        @Override public int getDefaultManaCost() { return 0; }
        @Override public int getCastingCost() { return 0; }
        @Override public boolean isEnabled() { return true; }
        @Override public SpellTier defaultTier() { return SpellTier.ONE; }
        @Override public SpellTier getConfigTier() { return SpellTier.ONE; }
        @Override public Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
        @Override public ResourceCharge firstCharge(SpellContext context, List<AbstractAugment> augments) {
            return account == null ? null : new ResourceCharge(account, 7);
        }
        @Override public void onResolve(HitResult hit, Level world, LivingEntity shooter, SpellStats stats,
                                        SpellContext context, SpellResolver resolver) {
            attempts++; last = CastContext.of(context, hit, null); policy = ActiveCastPolicy.of(context);
            if (account == null) successes++;
            else new ResourceCharge(last.account(), 7).trigger(() -> { successes++; return true; });
        }
    }
    private static SpellResolver parent(GameTestHelper h, LivingEntity caster, Spell spell, AtomicInteger resolutions) {
        var context = new SpellContext(h.getLevel(), spell, caster, LivingCaster.from(caster), ItemStack.EMPTY);
        new ActiveCastPolicy(false, false).attach(context);
        var resolver = new SpellResolver(context);
        ResolverContexts.attach(resolver, new Source(resolutions));
        return resolver;
    }
    public static void domain(GameTestHelper h) {
        var caster = cow(h, 3); var target = cow(h, 5); var account = new Account(); var calls = new AtomicInteger();
        var probe = new Probe(account);
        var resolver = parent(h, caster, new Spell(MethodTouch.INSTANCE, part("EffectDomain"), augment("OpenDomain"), probe), calls);
        CastContexts.bind(resolver.spellContext, CastContext.of(resolver.spellContext, null, account));
        resolver.onResolveEffect(h.getLevel(), new EntityHitResult(target));
        var domains = h.getLevel().getEntities(caster, caster.getBoundingBox().inflate(12),
                e -> e.getClass().getSimpleName().equals("EntityDomainSpell"));
        h.assertTrue(domains.size() == 1, "The native AAP domain was created");
        var saved = dev.arsconflux.api.glyph.ArsSpellAccess.resolver((EntityProjectileSpell)domains.get(0));
        h.assertTrue(saved.spellContext.getCurrentIndex() == 0 && !saved.spellContext.isCanceled(),
                "The sliced domain continuation starts at zero and can be made into child contexts");
        h.runAfterDelay(65, () -> {
            try {
                h.assertTrue(probe.attempts > 2 && probe.successes == 2 && account.balance == 0,
                        "Domain repeat billing: attempts=" + probe.attempts + ", successes=" + probe.successes + ", balance=" + account.balance);
                h.assertTrue(calls.get() > 0 && probe.last.caster() == caster && probe.last.damageOwner() == caster,
                        "Domain retains its source resolver and original ownership");
                h.assertTrue(!probe.policy.cooldowns() && !probe.policy.chanting(), "Entrypoint policy follows repeated domain effects");
                h.succeed();
            } finally { domains.forEach(Entity::discard); }
        });
    }
    public static void swap(GameTestHelper h) {
        var caster = cow(h, 2); var target = cow(h, 5); var calls = new AtomicInteger(); var probe = new Probe(null);
        var resolver = parent(h, caster, new Spell(MethodTouch.INSTANCE, part("EffectSwapTarget"), probe), calls);
        CastContexts.bind(resolver.spellContext, CastContext.of(resolver.spellContext, null, new Account()));
        resolver.onResolveEffect(h.getLevel(), new EntityHitResult(target));
        h.assertTrue(probe.successes == 1 && calls.get() == 1,
                "Swap Target source: successes=" + probe.successes + ", resolutions=" + calls.get());
        h.assertTrue(probe.last.caster() == target && probe.last.damageOwner() == target
                        && ((EntityHitResult)probe.last.target()).getEntity() == caster && probe.last.account() == null,
                "A fresh exchanged context uses AAP's new caster, rather than restoring stale ownership or an old account");
        h.assertTrue(!probe.policy.cooldowns() && !probe.policy.chanting(), "Entrypoint policy survives explicit context recreation");
        h.succeed();
    }
    public static void providers(GameTestHelper h) throws Exception {
        var two = augment("AmplifyTwo"); var three = augment("AmplifyThree");
        var levels = Class.forName("dev.ironsnouveau.casting.SpellLevels");
        h.assertTrue((int)levels.getMethod("delta", List.class).invoke(null, List.of(two, three, AugmentDampen.INSTANCE)) == 5,
                "Iron translates weighted equivalents into native spell levels");
        var fireball = GlyphRegistry.getSpellPart(CarrierRegistry.id("irons_nouveau:glyph_fireball"));
        h.assertTrue(fireball != null, "Iron projectile replacement is installed");
        var recipe = List.<AbstractSpellPart>of(MethodProjectile.INSTANCE, fireball, two, augment("AccelerateThree"));
        h.assertTrue(((List<?>)Class.forName("dev.ironsnouveau.casting.NativeCasting").getMethod("validate", List.class)
                .invoke(null, recipe)).isEmpty(), "Iron accepts high-tier modifiers on replacement projectiles");
        var validation = Class.forName("dev.goetynouveau.glyph.GoetyAugmentValidation");
        var focusClass = Class.forName("dev.goetynouveau.api.EffectFocus");
        var fangs = focusClass.getField("FANGS").get(null);
        var absorb = (AbstractAugment)Class.forName("dev.goetynouveau.glyph.AugmentGoety").getField("LIFE_ABSORB").get(null);
        h.assertTrue(!(boolean)validation.getMethod("validEffect", focusClass, List.class).invoke(null, fangs, List.of(absorb, two)),
                "High-tier Amplify cannot bypass Goety absorption restrictions");
        var errors = (List<?>)validation.getMethod("validate", List.class).invoke(null,
                List.of(MethodTouch.INSTANCE, GlyphRegistry.getSpellPart(CarrierRegistry.id("goety_nouveau:glyph_fangs")), absorb, two));
        h.assertTrue(errors.stream().anyMatch(e -> ((SpellValidationError)e).getPosition() == 3),
                "The incompatible high-tier augment is disabled at its actual palette slot");
        legacyGoetyBolt(h, three);
        h.succeed();
    }
    private static void legacyGoetyBolt(GameTestHelper h, AbstractAugment amplify) throws Exception {
        var config = Class.forName("dev.goetynouveau.platform.GoetyConfig").getField("RESOURCE_SOURCE").get(null);
        var get = config.getClass().getMethod("get"); var set = config.getClass().getMethod("set", Object.class);
        Object previous = get.invoke(config);
        Object ars = Arrays.stream(previous.getClass().getEnumConstants()).filter(e -> e.toString().equals("ARS_MANA")).findFirst().orElseThrow();
        var caster = cow(h, 3); var balance = new AtomicInteger(100000);
        var source = new LivingCaster(caster) {
            public boolean enoughMana(int amount) { return balance.get() >= amount; }
            public void expendMana(int amount) { balance.addAndGet(-amount); }
        };
        try {
            set.invoke(config, ars);
            int baseline = 0; int baselineDuration = 0;
            for (int pass = 0; pass < 2; pass++) {
                var payload = GlyphRegistry.getSpellPart(CarrierRegistry.id("goety_nouveau:glyph_magic_bolt"));
                Spell spell = pass == 0 ? new Spell(MethodProjectile.INSTANCE, payload)
                        : new Spell(MethodProjectile.INSTANCE, payload, amplify, augment("ExtendTimeThree"));
                var context = new SpellContext(h.getLevel(), spell, caster, source, ItemStack.EMPTY); context.nextPart();
                var carrier = new EntityProjectileSpell(h.getLevel(), new SpellResolver(context));
                carrier.setPos(caster.getEyePosition()); carrier.setDeltaMovement(1, 0, 0);
                h.assertTrue(ProjectileDispatch.convert(carrier), "Goety claims its native bolt");
                var nativeBolts = h.getLevel().getEntities(caster, caster.getBoundingBox().inflate(12), e -> e.getClass().getSimpleName().equals("MagicBolt"));
                h.assertTrue(nativeBolts.size() == 1, "One native Magic Bolt is spawned");
                int damage = ((Number)nativeBolts.get(0).getClass().getMethod("getExtraDamage").invoke(nativeBolts.get(0))).intValue();
                int duration = ((Number)nativeBolts.get(0).getClass().getMethod("getExtraDuration").invoke(nativeBolts.get(0))).intValue();
                if (pass == 0) { baseline = damage; baselineDuration = duration; }
                else h.assertTrue(damage == baseline + 4 && duration == baselineDuration + 4,
                        "Goety's legacy bolt receives four potency/duration points from tier-III augments");
                nativeBolts.forEach(Entity::discard);
            }
        } finally { set.invoke(config, previous); }
    }
}
