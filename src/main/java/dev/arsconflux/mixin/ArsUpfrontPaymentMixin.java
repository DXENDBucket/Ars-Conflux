package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pay Ars' remaining carrier cost before branches spend mana; keeps Ars 'n' Spells' own payment route. */
@Mixin(value = SpellResolver.class, remap = false)
public abstract class ArsUpfrontPaymentMixin {
    @Unique private boolean arsConflux$paid;
    @Inject(method = {"onCast", "onCastOnBlock", "onCastOnEntity"}, at = @At("HEAD"))
    private void arsConflux$mode(CallbackInfoReturnable<Boolean> ci) {
        dev.arsconflux.internal.billing.VanillaTriggerBilling.captureMode(((SpellResolver)(Object)this).spellContext);
    }
    @Inject(method = {"onCast", "onCastOnBlock", "onCastOnEntity"}, at = {
        @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/api/spell/AbstractCastMethod;onCast(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;)Lcom/hollingsworth/arsnouveau/api/spell/CastResolveType;"),
        @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/api/spell/AbstractCastMethod;onCastOnBlock(Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/LivingEntity;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;)Lcom/hollingsworth/arsnouveau/api/spell/CastResolveType;"),
        @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/api/spell/AbstractCastMethod;onCastOnBlock(Lnet/minecraft/world/item/context/UseOnContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;)Lcom/hollingsworth/arsnouveau/api/spell/CastResolveType;"),
        @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/api/spell/AbstractCastMethod;onCastOnEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;)Lcom/hollingsworth/arsnouveau/api/spell/CastResolveType;")})
    private void arsConflux$beforeDispatch(CallbackInfoReturnable<Boolean> ci) {
        var resolver = (SpellResolver)(Object)this;
        if (dev.arsconflux.api.glyph.ArsSpellAccess.level(resolver.spellContext) instanceof net.minecraft.server.level.ServerLevel
                && (resolver.spell.unsafeList().stream().anyMatch(p -> p instanceof dev.arsconflux.api.glyph.TriggerPaidGlyph)
                || dev.arsconflux.internal.billing.VanillaTriggerBilling.hasDeferred(resolver.spellContext))) {
            if (!(dev.arsconflux.api.glyph.ArsSpellAccess.caster(resolver.spellContext) instanceof net.minecraft.world.entity.player.Player player) || !player.isCreative())
                resolver.expendMana();
            arsConflux$paid = true;
            dev.arsconflux.api.projectile.VolleyBudget.begin(resolver);
        }
    }
    @WrapOperation(method = {"onCast", "onCastOnBlock", "onCastOnEntity"}, at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;expendMana()V"))
    private void arsConflux$once(SpellResolver resolver, Operation<Void> original) {
        if (!arsConflux$paid) original.call(resolver);
    }
    @Inject(method = {"onCast", "onCastOnBlock", "onCastOnEntity"}, at = @At("RETURN"))
    private void arsConflux$reset(CallbackInfoReturnable<Boolean> ci) {
        dev.arsconflux.api.projectile.VolleyBudget.finish((SpellResolver)(Object)this);
        arsConflux$paid = false;
    }
}
