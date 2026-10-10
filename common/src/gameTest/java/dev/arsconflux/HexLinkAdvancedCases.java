package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.effect.*;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.ArsTrajectory;
import dev.arsconflux.config.ConfluxConfig;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import java.lang.reflect.*;
import java.util.List;
import java.util.function.*;

/** Pinned real Linker/Hex runtime. Reflection remains in tests so absence stays testable. */
public final class HexLinkAdvancedCases {
    public record PlayerAccess(ServerPlayer player, IntConsumer setMana, DoubleSupplier mana) {}
    private HexLinkAdvancedCases() {}
    public static void verify(GameTestHelper h, PlayerAccess first, PlayerAccess second, boolean loaded) throws ReflectiveOperationException {
        if (!loaded) { h.succeed(); return; }
        var api = new Api(first.player());
        var ratio = api.config("Cfg_costRatePatternMana"); var media = api.config("Cfg_ratioExtraMediaCasting");
        var limit = api.config("Cfg_maxCallbackRecursionDepth");
        Object oldRatio = api.get(ratio), oldMedia = api.get(media), oldLimit = api.get(limit);
        boolean oldMode = ConfluxConfig.vanillaTriggerBilling();
        var area = first.player().getBoundingBox().inflate(24);
        try {
            api.set(ratio, .5); api.set(media, 2.0);
            first.setMana().accept(10000); second.setMana().accept(10000);
            var target = h.spawn(EntityType.COW, 8, 3, 2); target.setNoAi(true); target.setNoGravity(true);
            var hit = new EntityHitResult(target);
            var spell = new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE, AugmentAmplify.INSTANCE);
            ConfluxConfig.VANILLA_TRIGGER_BILLING.set(false);
            var full = api.resolver(SpellContext.fromEntity(spell, first.player(), ItemStack.EMPTY), api.environment, 0);
            int fullCost = full.getResolveCost();
            h.assertTrue(api.debit(full) == fullCost, "Requirement and native debit both use Linker mana multiplier");
            var child = full.getNewResolver(full.spellContext.clone());
            h.assertTrue(child.getClass() == full.getClass() && api.env(child) == api.environment,
                    "Ars derivation preserves the source resolver and environment");
            h.assertTrue(child.getResolveCost() == fullCost, "Derived resolver keeps the multiplier");

            var omitted = api.resolver(SpellContext.fromEntity(spell, first.player(), ItemStack.EMPTY),
                    api.environment, MethodProjectile.INSTANCE.getCastingCost());
            int fullMedia = api.media(omitted);
            ConfluxConfig.VANILLA_TRIGGER_BILLING.set(true);
            h.assertTrue(api.media(omitted) == fullMedia, "Deferred mana never reduces the full-recipe media quote");
            int upfront = api.debit(omitted);
            h.assertTrue(upfront == 0, "The artificial projectile method is omitted at launch");
            h.assertTrue(omitted.onCast(ItemStack.EMPTY, h.getLevel()), "Deferred Linker launch succeeds");
            int before = (int)first.mana().getAsDouble();
            omitted.onResolveEffect(h.getLevel(), hit);
            int effectCost = (int)((EffectHarm.INSTANCE.getCastingCost()
                    + AugmentAmplify.INSTANCE.getCostForPart(EffectHarm.INSTANCE)) * .5);
            h.assertTrue(before - first.mana().getAsDouble() == effectCost,
                    "Impact uses the multiplier and never omits the launch method again");
            target.invulnerableTime = 0; target.setHealth(target.getMaxHealth());
            first.setMana().accept(0);
            var empty = api.resolver(SpellContext.fromEntity(spell, first.player(), ItemStack.EMPTY),
                    api.environment, MethodProjectile.INSTANCE.getCastingCost());
            empty.onCast(ItemStack.EMPTY, h.getLevel()); empty.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(target.getHealth() == target.getMaxHealth(), "Empty balance refuses deferred native damage");
            first.player().setGameMode(GameType.CREATIVE); target.invulnerableTime = 0;
            var creative = api.resolver(SpellContext.fromEntity(spell, first.player(), ItemStack.EMPTY), api.environment, 0);
            creative.onCast(ItemStack.EMPTY, h.getLevel()); creative.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(target.getHealth() < target.getMaxHealth() && first.mana().getAsDouble() == 0,
                    "Creative remains free under deferred Linker casting");
            first.player().setGameMode(GameType.SURVIVAL); first.setMana().accept(10000);

            // Preserve callback depth through a collision-only resolver and a further derived context.
            api.set(limit, 1);
            Object callbackEnv = api.callback(first.player(), first.player().position(), full);
            var nested = api.resolver(SpellContext.fromEntity(spell, first.player(), ItemStack.EMPTY), callbackEnv, 0);
            var carrier = new EntityProjectileSpell(h.getLevel(), nested);
            carrier.setPos(first.player().getEyePosition());
            var visual = new Snowball(h.getLevel(), first.player());
            var motion = new ArsTrajectory(visual, carrier);
            try {
                h.assertTrue(motion.start(h.getLevel(), () -> {}, ignored -> {}), "Managed trajectory starts");
                var collision = ArsSpellAccess.resolver(carrier);
                var inherited = collision.getNewResolver(collision.spellContext.clone());
                Object next = api.callback(first.player(), first.player().position(), inherited);
                h.assertTrue(api.depth(next) == 2 && !api.ambit(next), "Callback depth and native limit survive conversion");
                var quote = ResolverContexts.quote(collision, collision.spellContext.clone());
                h.assertTrue(api.env(quote) == callbackEnv, "Collision wrappers retain their native quote factory");
            } finally { motion.close(); }

            // Actual Ars hit -> Linker callback -> Hex VM -> Ars target cast, including media payment.
            api.set(limit, 10); ConfluxConfig.VANILLA_TRIGGER_BILLING.set(false);
            first.player().getInventory().add(new ItemStack(Items.AMETHYST_SHARD, 64));
            int shards = count(first.player(), Items.AMETHYST_SHARD);
            Object glyphs = api.glyphList(EffectHarm.INSTANCE);
            api.storeCallback(first.player(), List.of(api.pattern("escape"), glyphs, api.pattern("touch")));
            var callbackPart = (AbstractSpellPart)Class.forName("io.yukkuric.hex_ars_link.glyph.HexCallbackSpellPart").getField("INSTANCE").get(null);
            var callbackSource = api.resolver(SpellContext.fromEntity(new Spell(MethodProjectile.INSTANCE, callbackPart),
                    first.player(), ItemStack.EMPTY), api.environment, 0);
            var callbackCarrier = new EntityProjectileSpell(h.getLevel(), callbackSource);
            callbackCarrier.setPos(first.player().getEyePosition());
            var callbackMotion = new ArsTrajectory(new Snowball(h.getLevel(), first.player()), callbackCarrier);
            target.invulnerableTime = 0; target.setHealth(target.getMaxHealth());
            try {
                callbackMotion.start(h.getLevel(), () -> {}, impact -> {
                    var continuation = callbackSource.getNewResolver(callbackSource.spellContext.clone());
                    continuation.onResolveEffect(h.getLevel(), impact);
                });
                ArsSpellAccess.resolver(callbackCarrier).onResolveEffect(h.getLevel(), hit);
                h.assertTrue(target.getHealth() < target.getMaxHealth(), "Real callback VM resolves an Ars effect on the hit entity");
                h.assertTrue(count(first.player(), Items.AMETHYST_SHARD) < shards, "Callback VM pays native Hex media");
            } finally { callbackMotion.close(); api.storeCallback(first.player(), List.of()); }

            // Delegate a spell: Hex runner pays media, selected player remains Ars caster/payer.
            var receiver = second.player(); receiver.setHealth(2); h.getLevel().addNewPlayer(receiver);
            int sourceMana = (int)first.mana().getAsDouble(); int receiverMana = (int)second.mana().getAsDouble();
            int sourceMedia = count(first.player(), Items.AMETHYST_SHARD);
            api.run(api.environment, List.of(api.entity(receiver), api.glyphList(MethodSelf.INSTANCE, EffectHeal.INSTANCE)),
                    api.pattern("delegate"), h.getLevel());
            h.assertTrue(receiver.getHealth() > 2 && second.mana().getAsDouble() < receiverMana,
                    "Delegate receives native healing and pays their own Ars mana");
            h.assertTrue(first.mana().getAsDouble() == sourceMana, "Hex runner never pays the delegate's Ars mana");
            h.assertTrue(count(first.player(), Items.AMETHYST_SHARD) < sourceMedia, "Hex runner pays delegated casting media");
            // The full VM's specified-block path exercises the same native target route.
            api.run(api.environment, List.of(api.vector(first.player().position()), api.glyphList(EffectLight.INSTANCE)),
                    api.pattern("touch"), h.getLevel());
            h.succeed();
        } finally {
            first.player().setGameMode(GameType.SURVIVAL);
            api.set(ratio, oldRatio); api.set(media, oldMedia); api.set(limit, oldLimit);
            ConfluxConfig.VANILLA_TRIGGER_BILLING.set(oldMode);
            h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, area,
                    p -> ArsSpellAccess.resolver(p) != null && ArsSpellAccess.resolver(p).spellContext.getUnwrappedCaster() == first.player()).forEach(Entity::discard);
            second.player().discard();
        }
    }
    private static int count(ServerPlayer player, Item item) {
        int count = 0; for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i); if (stack.is(item)) count += stack.getCount();
        } return count;
    }
    private static final class Api {
        final Class<?> envType = Class.forName("at.petrak.hexcasting.api.casting.eval.CastingEnvironment");
        final Class<?> patternResolver = Class.forName("io.yukkuric.hex_ars_link.env.ars.PatternResolver");
        final Class<?> callback = Class.forName("io.yukkuric.hex_ars_link.env.hex.GlyphCallbackCastEnv");
        final Class<?> iota = Class.forName("at.petrak.hexcasting.api.casting.iota.Iota");
        final Class<?> tree = Class.forName("at.petrak.hexcasting.api.utils.TreeList");
        final Object environment;
        final ServerLevel world;
        Api(ServerPlayer player) throws ReflectiveOperationException {
            world = player.serverLevel();
            environment = Class.forName("at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv")
                    .getConstructor(ServerPlayer.class, InteractionHand.class).newInstance(player, InteractionHand.MAIN_HAND);
        }
        Object config(String field) throws ReflectiveOperationException { return Class.forName("io.yukkuric.hex_ars_link.config.LinkConfigForge").getField(field).get(null); }
        Object get(Object value) throws ReflectiveOperationException { return value.getClass().getMethod("get").invoke(value); }
        void set(Object value, Object setting) throws ReflectiveOperationException { value.getClass().getMethod("set", Object.class).invoke(value, setting); }
        SpellResolver resolver(SpellContext context, Object env, int omit) throws ReflectiveOperationException {
            return (SpellResolver)patternResolver.getConstructor(SpellContext.class, envType, int.class).newInstance(context, env, omit);
        }
        Object env(SpellResolver resolver) throws ReflectiveOperationException { return patternResolver.getMethod("getEnv").invoke(resolver); }
        int media(SpellResolver resolver) throws ReflectiveOperationException { return (int)patternResolver.getMethod("getMediaCost").invoke(resolver); }
        int debit(SpellResolver resolver) throws ReflectiveOperationException {
            try { return (int)SpellResolver.class.getMethod("getExpendedCost").invoke(resolver); }
            catch (NoSuchMethodException ars4) { return resolver.getResolveCost(); }
        }
        Object callback(ServerPlayer player, Vec3 pos, SpellResolver source) throws ReflectiveOperationException {
            return callback.getConstructor(ServerPlayer.class, Vec3.class, SpellResolver.class).newInstance(player, pos, source);
        }
        int depth(Object env) throws ReflectiveOperationException { return (int)callback.getMethod("getRecursionDepth").invoke(env); }
        boolean ambit(Object env) throws ReflectiveOperationException { return (boolean)callback.getMethod("getHasAmbit").invoke(env); }
        Object entity(Entity entity) throws ReflectiveOperationException {
            return Class.forName("at.petrak.hexcasting.api.casting.iota.EntityIota").getConstructor(Entity.class).newInstance(entity);
        }
        Object vector(Vec3 pos) throws ReflectiveOperationException {
            return Class.forName("at.petrak.hexcasting.api.casting.iota.Vec3Iota").getConstructor(Vec3.class).newInstance(pos);
        }
        Object glyphList(AbstractSpellPart... parts) throws ReflectiveOperationException {
            var constructor = Class.forName("io.yukkuric.hex_ars_link.iota.GlyphIota").getConstructor(AbstractSpellPart.class);
            var list = new java.util.ArrayList<>(); for (var part : parts) list.add(constructor.newInstance(part));
            return Class.forName("at.petrak.hexcasting.api.casting.iota.ListIota").getConstructor(List.class).newInstance(list);
        }
        Object pattern(String name) throws ReflectiveOperationException {
            String angles = switch (name) {
                case "touch" -> "wwqwaawewaawdwawwawwqwwaww";
                case "delegate" -> "aqaeqwaawewaawdwawwawwqwwawwwded";
                case "escape" -> "qqqaw";
                default -> throw new IllegalArgumentException(name);
            };
            var dir = Class.forName("at.petrak.hexcasting.api.casting.math.HexDir");
            Object facing = dir.getField(name.equals("delegate") ? "NORTH_WEST" : name.equals("escape") ? "WEST" : "EAST").get(null);
            var pattern = Class.forName("at.petrak.hexcasting.api.casting.math.HexPattern");
            Object hex;
            try { hex = pattern.getMethod("fromAngleString", String.class, dir, boolean.class).invoke(null, angles, facing, false); }
            catch (NoSuchMethodException hex11) {
                hex = pattern.getMethod("fromAngles", String.class, dir).invoke(null, angles, facing);
            }
            if (name.equals("delegate")) {
                // Linker's Forge release marks delegation as a per-world great spell.
                var xplat = Class.forName("at.petrak.hexcasting.xplat.IXplatAbstractions");
                @SuppressWarnings("unchecked")
                var registry = (net.minecraft.core.Registry<Object>)xplat.getMethod("getActionRegistry")
                        .invoke(xplat.getField("INSTANCE").get(null));
                var key = registry.getResourceKey(registry.get(dev.arsconflux.api.projectile.CarrierRegistry.id("hex_ars_link:cast_spell_as"))).orElseThrow();
                Object canonical = Class.forName("at.petrak.hexcasting.common.casting.PatternRegistryManifest")
                        .getMethod("getCanonicalStrokesPerWorld", net.minecraft.resources.ResourceKey.class, ServerLevel.class)
                        .invoke(null, key, world);
                if (canonical != null) hex = canonical;
            }
            return Class.forName("at.petrak.hexcasting.api.casting.iota.PatternIota").getConstructor(pattern).newInstance(hex);
        }
        Object tree(List<?> list) throws ReflectiveOperationException { return tree.getMethod("from", Iterable.class).invoke(null, list); }
        void storeCallback(ServerPlayer player, List<?> list) throws ReflectiveOperationException {
            Object program = Class.forName("at.petrak.hexcasting.api.casting.iota.ListIota")
                    .getConstructor(List.class).newInstance(list);
            Object env = Class.forName("at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv")
                    .getConstructor(ServerPlayer.class, InteractionHand.class).newInstance(player, InteractionHand.MAIN_HAND);
            var action = Class.forName("io.yukkuric.hex_ars_link.action.OpSetCallback");
            action.getMethod("execute", List.class, envType).invoke(action.getField("INSTANCE").get(null), List.of(program), env);
        }
        void run(Object env, List<?> stack, Object action, ServerLevel world) throws ReflectiveOperationException {
            var imageType = Class.forName("at.petrak.hexcasting.api.casting.eval.vm.CastingImage");
            Object image;
            try {
                image = imageType.getConstructor(tree, int.class, tree, boolean.class, boolean.class, long.class, CompoundTag.class)
                        .newInstance(tree(stack), 0, tree(List.of()), false, false, 0L, new CompoundTag());
            } catch (NoSuchMethodException hex11) {
                image = imageType.getConstructor(List.class, int.class, List.class, boolean.class, long.class, CompoundTag.class)
                        .newInstance(stack, 0, List.of(), false, 0L, new CompoundTag());
            }
            var vmType = Class.forName("at.petrak.hexcasting.api.casting.eval.vm.CastingVM");
            var vm = vmType.getConstructor(imageType, envType).newInstance(image, env);
            Object view = vmType.getMethod("queueExecuteAndWrapIota", iota, ServerLevel.class).invoke(vm, action, world);
            Object resolution = view.getClass().getMethod("getResolutionType").invoke(view);
            if (!(boolean)resolution.getClass().getMethod("getSuccess").invoke(resolution))
                throw new IllegalStateException("Hex VM evaluation failed for " + action + ": " + view
                        + "; enlightened=" + envType.getMethod("isEnlightened").invoke(env));
        }
    }
}
