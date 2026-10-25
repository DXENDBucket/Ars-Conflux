package dev.arsconflux.mixin;

import dev.arsconflux.internal.ans.AnsActiveCasts;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.otectus.arsnspells.spell.CrossCastingHandler", remap = false)
public abstract class OptionalAnsCarrierMixin {
    @Inject(method = "castArsSpell(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lcom/otectus/arsnspells/spell/CrossModSpell;Ljava/util/UUID;Lnet/minecraft/world/InteractionHand;Lcom/otectus/arsnspells/spell/target/CastTargetHint;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void arsConflux$active(Player player, ItemStack stack, @Coerce Object entry, UUID attempt,
            InteractionHand hand, @Coerce Object hint, CallbackInfoReturnable<Boolean> ci) {
        Object result = AnsActiveCasts.carrier(player, stack, stack, hand, entry,
                new Object[]{player, stack, entry, attempt, hand, hint});
        if (result != null) ci.setReturnValue((Boolean)result);
    }
}
