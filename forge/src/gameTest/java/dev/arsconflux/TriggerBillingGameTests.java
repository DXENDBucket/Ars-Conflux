package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityOrbitProjectile;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.effect.*;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.arsconflux.config.ConfluxConfig;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.gametest.*;

@GameTestHolder("ars_conflux_billing") @PrefixGameTestTemplate(false)
public final class TriggerBillingGameTests {
    @GameTest(template="empty", timeoutTicks=40)
    public static void nativeArs4PaymentsAndNestedOrbit(GameTestHelper h) {
        boolean previous = ConfluxConfig.vanillaTriggerBilling();
        try {
            var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ConfluxBilling"));
            player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2, 3, 2)));
            player.setYRot(-90); player.setGameMode(GameType.SURVIVAL);
            var mana = CapabilityRegistry.getMana(player).orElseThrow(IllegalStateException::new);
            mana.setMaxMana(10000); mana.setMana(10000);
            var target = h.spawn(EntityType.COW, 8, 3, 2); target.setNoAi(true); target.setNoGravity(true);
            var hit = new EntityHitResult(target);
            var spell = new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE, AugmentAmplify.INSTANCE);
            ConfluxConfig.VANILLA_TRIGGER_BILLING.set(false);
            var original = new SpellResolver(SpellContext.fromEntity(spell, player, ItemStack.EMPTY));
            int full = original.getResolveCost();
            h.assertTrue(original.onCast(ItemStack.EMPTY, h.getLevel()), "Native Ars 4 launch succeeds");
            original.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(10000 - mana.getCurrentMana() == full, "Disabled mode pays once upfront");

            ConfluxConfig.VANILLA_TRIGGER_BILLING.set(true); mana.setMana(10000);
            var paid = new SpellResolver(SpellContext.fromEntity(spell, player, ItemStack.EMPTY));
            int launch = paid.getResolveCost();
            h.assertTrue(paid.onCast(ItemStack.EMPTY, h.getLevel()), "Deferred launch succeeds");
            h.assertTrue(launch < full && 10000 - mana.getCurrentMana() == launch, "Only carrier paid at launch");
            paid.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(10000 - mana.getCurrentMana() == full, "Native capability pays the effect and augment once");
            mana.setMana(0); target.setHealth(target.getMaxHealth()); target.invulnerableTime=0;
            paid.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(target.getHealth() == target.getMaxHealth(), "Empty mana blocks repeated damage");

            int cost = EffectOrbit.INSTANCE.getCastingCost(); mana.setMana(cost * 2);
            var nested = new SpellResolver(SpellContext.fromEntity(
                    new Spell(MethodSelf.INSTANCE, EffectOrbit.INSTANCE, EffectOrbit.INSTANCE, EffectHarm.INSTANCE), player, ItemStack.EMPTY));
            nested.onResolveEffect(h.getLevel(), new EntityHitResult(player));
            var box = player.getBoundingBox().inflate(20);
            var outer = h.getLevel().getEntitiesOfClass(EntityOrbitProjectile.class, box);
            h.assertTrue(outer.size() == 3 && mana.getCurrentMana() == cost, "First native Orbit paid");
            for (var ball : outer) ball.spellResolver.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(mana.getCurrentMana() == 0 && h.getLevel().getEntitiesOfClass(EntityOrbitProjectile.class, box).size() == 6,
                    "Only one child creates more native Orbit balls");

            player.setGameMode(GameType.CREATIVE); mana.setMana(0);
            var creative = new SpellResolver(SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE, EffectHeal.INSTANCE), player, ItemStack.EMPTY));
            player.setHealth(1); creative.onCast(ItemStack.EMPTY, h.getLevel());
            h.assertTrue(player.getHealth() > 1 && mana.getCurrentMana() == 0, "Creative pays neither launch nor effect mana");
            h.succeed();
        } finally { ConfluxConfig.VANILLA_TRIGGER_BILLING.set(previous); }
    }
}
