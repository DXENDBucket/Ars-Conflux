package dev.arsconflux.mixin;

import dev.arsconflux.internal.ans.AnsActiveCasts;
import net.minecraft.nbt.CompoundTag;
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
    @Inject(method = "castArsSpell(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/nbt/CompoundTag;Ljava/util/UUID;Lcom/otectus/arsnspells/spell/target/InteractionOrigin;Lcom/otectus/arsnspells/spell/target/TargetHint;)Lcom/otectus/arsnspells/spell/ArsCastDispatcher$Result;",
            at = @At("HEAD"), cancellable = true)
    private static void arsConflux$active(Player player, ItemStack carrier, ItemStack tool, InteractionHand hand,
            CompoundTag entry, UUID attempt, @Coerce Object origin, @Coerce Object hint, CallbackInfoReturnable<Object> ci) {
        Object result = AnsActiveCasts.carrier(player, carrier, tool, hand, entry,
                new Object[]{player, carrier, tool, hand, entry, attempt, origin, hint});
        if (result != null) ci.setReturnValue(result);
    }
}
