package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.effect.*;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import com.hollingsworth.arsnouveau.common.entity.EntityOrbitProjectile;
import dev.arsconflux.config.ConfluxConfig;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.projectile.CarrierRegistry;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** One isolated batch switches the server config and restores it, avoiding cross-test config races. */
@GameTestHolder("ars_conflux_billing") @PrefixGameTestTemplate(false)
public final class TriggerBillingGameTests {
    private static LivingEntity cow(GameTestHelper h, int x) {
        var cow = h.spawn(EntityType.COW, x, 3, 2);
        cow.setNoAi(true); cow.setNoGravity(true); cow.setYRot(-90); return cow;
    }
    private static SpellResolver resolver(GameTestHelper h, LivingEntity caster, AtomicInteger balance, Spell spell) {
        var source = new LivingCaster(caster) {
            public boolean enoughMana(int amount) { return balance.get() >= amount; }
            public void expendMana(int amount) { balance.addAndGet(-amount); }
        };
        return new SpellResolver(new SpellContext(h.getLevel(), spell, caster, source, ItemStack.EMPTY));
    }
    @GameTest(template="empty", timeoutTicks=40)
    public static void defaultBillingDeferredHitsBudgetAndNestedOrbit(GameTestHelper h) {
        boolean previous = ConfluxConfig.vanillaTriggerBilling();
        try {
            h.assertTrue(!ConfluxConfig.VANILLA_TRIGGER_BILLING.getDefault(), "Native behavior is the default");
            var caster = cow(h, 2); var target = cow(h, 8);
            var balance = new AtomicInteger(10000);
            var spell = new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE, AugmentAmplify.INSTANCE);
            ConfluxConfig.VANILLA_TRIGGER_BILLING.set(false);
            var original = resolver(h, caster, balance, spell); int full = original.getExpendedCost();
            int before = balance.get();
            h.assertTrue(original.onCast(ItemStack.EMPTY, h.getLevel()), "Native launch succeeds");
            h.assertTrue(before - balance.get() == full, "Disabled mode pays native full cost on launch");
            var hit = new EntityHitResult(target);
            original.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(before - balance.get() == full, "Disabled mode never adds impact charges");

            ConfluxConfig.VANILLA_TRIGGER_BILLING.set(true);
            before = balance.get(); original.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(balance.get() == before, "A pre-reload native cast never gains a second charge");
            balance.set(10000); target.setHealth(target.getMaxHealth()); target.invulnerableTime = 0;
            var deferred = resolver(h, caster, balance, spell); int launch = deferred.getExpendedCost();
            h.assertTrue(launch < full, "Effect plus augment removed from both launch quote phases");
            h.assertTrue(deferred.onCast(ItemStack.EMPTY, h.getLevel()), "Deferred launch succeeds");
            h.assertTrue(10000 - balance.get() == launch, "A projectile with no impact pays only the launch cost");
            deferred.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(10000 - balance.get() == full, "One effect plus augment costs exactly once after launch");
            int first = balance.get();
            ConfluxConfig.VANILLA_TRIGGER_BILLING.set(false);
            deferred.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(first - balance.get() == full - launch, "Repeated carrier hits pay separately");
            ConfluxConfig.VANILLA_TRIGGER_BILLING.set(true);

            target.invulnerableTime = 0; target.setHealth(target.getMaxHealth());
            balance.set(launch);
            var empty = resolver(h, caster, balance, spell);
            h.assertTrue(empty.onCast(ItemStack.EMPTY, h.getLevel()), "Launch needs only its own mana");
            float health = target.getHealth(); empty.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(balance.get() == 0 && target.getHealth() == health && !empty.spellContext.hasNextPart(),
                    "Insufficient trigger mana does not apply damage or leave a continuation running");

            var foreign = new ForeignEffect();
            balance.set(10000);
            var mixed = resolver(h, caster, balance, new Spell(MethodProjectile.INSTANCE, foreign, EffectHarm.INSTANCE));
            h.assertTrue(mixed.getExpendedCost() == MethodProjectile.INSTANCE.getCastingCost() + foreign.getCastingCost(),
                    "Addon effects keep their launch costs; no automatic double billing");

            // Actual native Orbit creation, then invoke each child resolver as a carrier impact would.
            int orbitCost = EffectOrbit.INSTANCE.getCastingCost();
            balance.set(orbitCost * 2);
            var nested = resolver(h, caster, balance,
                    new Spell(MethodSelf.INSTANCE, EffectOrbit.INSTANCE, EffectOrbit.INSTANCE, EffectHarm.INSTANCE));
            nested.onResolveEffect(h.getLevel(), new EntityHitResult(caster));
            var box = caster.getBoundingBox().inflate(20);
            var outer = h.getLevel().getEntitiesOfClass(EntityOrbitProjectile.class, box);
            h.assertTrue(outer.size() == 3 && balance.get() == orbitCost, "First paid Orbit creates the normal three balls");
            for (var ball : outer) ball.resolver().onResolveEffect(h.getLevel(), new EntityHitResult(target));
            h.assertTrue(balance.get() == 0, "Only one nested Orbit can be paid");
            h.assertTrue(h.getLevel().getEntitiesOfClass(EntityOrbitProjectile.class, box).size() == 6,
                    "Unfunded children spawn no nested balls instead of growing to twelve");
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ConfluxCreative"));
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            balance.set(0); player.setHealth(1);
            var creative = resolver(h, player, balance, new Spell(MethodSelf.INSTANCE, EffectHeal.INSTANCE));
            h.assertTrue(creative.onCast(ItemStack.EMPTY, h.getLevel()), "Creative casts at zero mana");
            h.assertTrue(player.getHealth() > 1 && balance.get() == 0, "Creative pays neither launch nor effect mana");
            h.succeed();
        } finally { ConfluxConfig.VANILLA_TRIGGER_BILLING.set(previous); }
    }
    private static final class ForeignEffect extends AbstractEffect implements TriggerPaidGlyph {
        ForeignEffect() { super(CarrierRegistry.id("other_provider:glyph_test"), "Other provider"); }
        public int getDefaultManaCost() { return 7; }
        public int getCastingCost() { return 7; }
        public boolean isEnabled() { return true; }
        public SpellTier getConfigTier() { return SpellTier.ONE; }
        public Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
    }
}
