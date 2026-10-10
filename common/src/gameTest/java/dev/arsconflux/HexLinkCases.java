package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.mojang.datafixers.util.Either;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.glyph.*;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.*;
import dev.arsconflux.config.ConfluxConfig;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Real Linker actions, two unrelated Core providers, no Iron or Goety dependency.
 * Reflection belongs only to the integration fixture, not to addon production code. */
public final class HexLinkCases {
    private HexLinkCases() {}
    public static void verify(GameTestHelper h, ServerPlayer player, boolean linkerLoaded, java.util.function.IntConsumer setMana) throws ReflectiveOperationException {
        if (!linkerLoaded) {
            h.assertTrue(!SpellValidators.validate(List.of(MethodProjectile.INSTANCE,
                    new TestForm("fixture_a:glyph_hex_payload"), new TestForm("fixture_b:glyph_hex_payload"))).isEmpty(),
                    "Shared provider rules load without Hex classes");
            h.succeed(); return;
        }
        boolean previous = ConfluxConfig.vanillaTriggerBilling();
        ConfluxConfig.VANILLA_TRIGGER_BILLING.set(false);
        var a = new TestForm("fixture_a:glyph_hex_payload");
        var b = new TestForm("fixture_b:glyph_hex_payload");
        MethodProjectile.INSTANCE.compatibleAugments.add(a);
        MethodProjectile.INSTANCE.compatibleAugments.add(b);
        try {
            setMana.accept(10000);
            var target = h.spawn(EntityType.COW, 8, 3, 2);
            target.setNoAi(true); target.setNoGravity(true);
            var envType = Class.forName("at.petrak.hexcasting.api.casting.eval.CastingEnvironment");
            var env = Class.forName("at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv")
                    .getConstructor(ServerPlayer.class, InteractionHand.class).newInstance(player, InteractionHand.MAIN_HAND);
            var patternType = Class.forName("io.yukkuric.hex_ars_link.env.ars.PatternResolver");
            var patternConstructor = patternType.getConstructor(SpellContext.class, envType, int.class);
            var casterType = Class.forName("io.yukkuric.hex_ars_link.env.ars.PatternCaster");
            Object caster;
            try {
                caster = casterType.getConstructor(envType).newInstance(env);
            } catch (NoSuchMethodException olderLinker) {
                caster = casterType.getConstructor(ItemStack.class, envType).newInstance(ItemStack.EMPTY, env);
            }
            // Ars 4 accepts ISpellCaster; Ars 5 accepts SpellCaster. Keep that difference out of the fixture logic.
            var normalType = Class.forName("io.yukkuric.hex_ars_link.action.OpCastFromPlayer$Action");
            var normalConstructor = java.util.Arrays.stream(normalType.getConstructors())
                    .filter(c -> c.getParameterCount() == 3 && c.getParameterTypes()[0] == Spell.class)
                    .findFirst().orElseThrow();
            var shootType = Class.forName("io.yukkuric.hex_ars_link.action.OpShootCast$Action");
            var shootConstructor = shootType.getConstructor(Vec3.class, Vec3.class, patternType);
            var touchType = Class.forName("io.yukkuric.hex_ars_link.action.OpTouchCast$Action");
            var touchConstructor = touchType.getConstructor(Either.class, patternType);
            var area = player.getBoundingBox().inflate(24);
            for (var form : List.of(a, b)) {
                var spell = new Spell(MethodProjectile.INSTANCE, form);
                var normal = normalConstructor.newInstance(spell, caster, player);
                normalType.getMethod("cast", envType).invoke(normal, env);
                h.assertTrue(form.calls.get() == 1, "Native Linker regular cast dispatches to this provider");
                h.assertTrue(h.getLevel().getEntitiesOfClass(Snowball.class, area).size() == 1, "One replacement from regular cast");
                h.getLevel().getEntitiesOfClass(Snowball.class, area).forEach(Entity::discard);

                var direction = new Vec3(1, -.25, .5).normalize();
                var context = SpellContext.fromEntity(spell, player, ItemStack.EMPTY);
                var attribution = CastContext.of(context, null, null).withDamageOwner(target).withIncomingDirection(direction);
                CastContexts.bind(context, attribution);
                var resolver = (SpellResolver)patternConstructor.newInstance(context, env, MethodProjectile.INSTANCE.getCastingCost());
                var origin = player.getEyePosition().add(0, 1, 0);
                var positioned = shootConstructor.newInstance(origin, direction, resolver);
                shootType.getMethod("cast", envType).invoke(positioned, env);
                h.assertTrue(form.calls.get() == 2, "Native Linker positioned cast dispatches to this provider exactly once");
                var replacements = h.getLevel().getEntitiesOfClass(Snowball.class, area);
                h.assertTrue(replacements.size() == 1 && replacements.get(0).getOwner() == player, "Owner remains the caster");
                h.assertTrue(replacements.get(0).position().distanceTo(origin) < .01, "Requested launch position retained");
                h.assertTrue(replacements.get(0).getDeltaMovement().normalize().dot(direction) > .999, "Requested direction retained");
                h.assertTrue(form.last != null && form.last.caster() == player && form.last.damageOwner() == target,
                        "Independent provider reads shared attribution rather than Linker-specific state");
                var cloned = CastContext.of(context.clone(), null, null);
                h.assertTrue(cloned.damageOwner() == target && cloned.incomingDirection().dot(direction) > .999,
                        "Clone preserves common attribution and direction");
                h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, area).isEmpty(), "No duplicate original carrier");
                replacements.forEach(Entity::discard);

                var effect = new TestEffect(form.getRegistryName().getNamespace() + ":glyph_hex_effect");
                var touchContext = SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE, effect), player, ItemStack.EMPTY);
                var touchResolver = patternConstructor.newInstance(touchContext, env, MethodTouch.INSTANCE.getCastingCost());
                var touch = touchConstructor.newInstance(Either.left(target), touchResolver);
                touchType.getMethod("cast", envType).invoke(touch, env);
                h.assertTrue(effect.calls.get() == 1 && effect.last.target() instanceof EntityHitResult hit && hit.getEntity() == target,
                        "Native Linker entity cast reaches an independent effect provider with the chosen target");
                h.assertTrue(effect.last.caster() == player && effect.last.executor() == player,
                        "Choosing a target does not transfer ownership or payment");

                int before = form.calls.get(); setMana.accept(0);
                var emptyContext = SpellContext.fromEntity(spell, player, ItemStack.EMPTY);
                var empty = patternConstructor.newInstance(emptyContext, env, MethodProjectile.INSTANCE.getCastingCost());
                var emptyAction = shootConstructor.newInstance(origin, direction, empty);
                shootType.getMethod("cast", envType).invoke(emptyAction, env);
                h.assertTrue(form.calls.get() == before && h.getLevel().getEntitiesOfClass(Snowball.class, area).isEmpty(),
                        "Provider resource refusal still applies to native Linker positioned casts");
                setMana.accept(10000);
            }
            h.assertTrue(a.calls.get() == 2 && b.calls.get() == 2, "Both independent providers work without knowing one another or Hex");
            h.succeed();
        } finally {
            MethodProjectile.INSTANCE.compatibleAugments.remove(a);
            MethodProjectile.INSTANCE.compatibleAugments.remove(b);
            ConfluxConfig.VANILLA_TRIGGER_BILLING.set(previous);
        }
    }
    private static final class TestForm extends AbstractAugment implements ProjectileForm {
        final AtomicInteger calls = new AtomicInteger();
        CastContext last;
        TestForm(String id) { super(CarrierRegistry.id(id), id); }
        public int getDefaultManaCost() { return 0; }
        public int getCastingCost() { return 0; }
        public boolean isEnabled() { return true; }
        public SpellTier getConfigTier() { return SpellTier.ONE; }
        public int getCostForPart(AbstractSpellPart part) { return 0; }
        public ResourceCharge initialCharge(SpellContext context, List<AbstractAugment> augments) {
            return new ResourceCharge(new ArsManaAccount(context.getCaster()), 7);
        }
        public boolean replace(EntityProjectileSpell projectile, CarrierProfile profile) {
            var ars = ArsSpellAccess.resolver(projectile).spellContext;
            var account = new ArsManaAccount(ars.getCaster());
            initialCharge(ars, List.of()).trigger(() -> {
                var visual = new Snowball(projectile.level(), ars.getUnwrappedCaster());
                visual.setPos(projectile.position()); visual.setDeltaMovement(projectile.getDeltaMovement());
                boolean spawned = projectile.level().addFreshEntity(visual);
                if (spawned) { last = CastContext.of(ars, null, account); calls.incrementAndGet(); }
                return spawned;
            });
            return true;
        }
    }
    private static final class TestEffect extends AbstractEffect implements TriggerPaidGlyph {
        final AtomicInteger calls = new AtomicInteger();
        CastContext last;
        TestEffect(String id) { super(CarrierRegistry.id(id), id); }
        public java.util.Set<AbstractAugment> getCompatibleAugments() { return java.util.Set.of(); }
        public int getDefaultManaCost() { return 0; }
        public int getCastingCost() { return 0; }
        public boolean isEnabled() { return true; }
        public SpellTier getConfigTier() { return SpellTier.ONE; }
        @Override public void onResolve(HitResult hit, Level world, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) {
            var account = new ArsManaAccount(context.getCaster());
            new ResourceCharge(account, 7).trigger(() -> { last = CastContext.of(context, hit, account); calls.incrementAndGet(); return true; });
        }
    }
}
