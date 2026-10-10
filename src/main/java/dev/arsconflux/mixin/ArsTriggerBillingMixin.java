package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.ManaUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.arsconflux.internal.billing.VanillaTriggerBilling;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpellResolver.class, remap = false)
public abstract class ArsTriggerBillingMixin {
    @Inject(method = "getResolveCost", at = @At("HEAD"), cancellable = true)
    private void arsConflux$frozenRequirement(CallbackInfoReturnable<Integer> ci) {
        var cost = VanillaTriggerBilling.frozenQuoteCost(((SpellResolver)(Object)this).spellContext, false);
        if (cost != null) ci.setReturnValue(cost);
    }
    @Inject(method = "getExpendedCost", at = @At("HEAD"), cancellable = true)
    private void arsConflux$frozenDebit(CallbackInfoReturnable<Integer> ci) {
        var cost = VanillaTriggerBilling.frozenQuoteCost(((SpellResolver)(Object)this).spellContext, true);
        if (cost != null) ci.setReturnValue(cost);
    }
    @WrapOperation(method = {"getResolveCost", "getExpendedCost"}, at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/spell/Spell;getCost()I", remap = false))
    private int arsConflux$launchCost(Spell spell, Operation<Integer> original) {
        return VanillaTriggerBilling.upfrontRaw(((SpellResolver)(Object)this).spellContext, original.call(spell));
    }
    @WrapOperation(method = {"getResolveCost", "getExpendedCost"}, at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/util/ManaUtil;getPlayerDiscounts(Lnet/minecraft/world/entity/LivingEntity;Lcom/hollingsworth/arsnouveau/api/spell/Spell;Lnet/minecraft/world/item/ItemStack;)I", remap = false))
    private int arsConflux$launchDiscount(LivingEntity caster, Spell spell, ItemStack tool, Operation<Integer> original) {
        return VanillaTriggerBilling.upfrontDiscount(((SpellResolver)(Object)this).spellContext, original.call(caster, spell, tool));
    }
    @WrapOperation(method = "resume", at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/spell/AbstractEffect;onResolve(Lnet/minecraft/world/phys/HitResult;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;)V", remap = false))
    private void arsConflux$dispatch(AbstractEffect effect, HitResult hit, Level level, LivingEntity caster,
            SpellStats stats, SpellContext context, SpellResolver resolver, Operation<Void> original) {
        VanillaTriggerBilling.resolve(effect, resolver, hit, () -> original.call(effect, hit, level, caster, stats, context, resolver));
    }
}
