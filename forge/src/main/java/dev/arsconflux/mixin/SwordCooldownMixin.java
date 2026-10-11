package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.items.EnchantersSword;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsconflux.api.casting.ActiveCasts;
import dev.arsconflux.api.casting.ActiveCastSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = EnchantersSword.class, remap = false)
public abstract class SwordCooldownMixin {
    @WrapOperation(method = "hurtEnemy", remap = true, at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;onCastOnEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Z", remap = false))
    private boolean arsConflux$magicOnly(SpellResolver resolver, ItemStack stack, Entity target, InteractionHand hand, Operation<Boolean> original) {
        return ActiveCasts.execute(resolver.spell, dev.arsconflux.api.glyph.ArsSpellAccess.caster(resolver.spellContext), ActiveCastSource.WEAPON,
                () -> original.call(resolver, stack, target, hand), false);
    }
}
