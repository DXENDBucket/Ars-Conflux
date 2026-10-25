package dev.arsconflux.mixin;

import dev.arsconflux.internal.ans.AnsActiveCasts;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only ANS proxy instances opt in. All native Iron spells pass through. */
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.api.spells.AbstractSpell", remap = false)
public abstract class OptionalAnsNativeCastMixin {
    @Inject(method = "attemptInitiateCast", at = @At("HEAD"), cancellable = true)
    private void arsConflux$prepare(ItemStack stack, int level, Level world, Player player,
            @Coerce Object source, boolean cooldown, String slot, CallbackInfoReturnable<Boolean> ci) {
        Boolean result = AnsActiveCasts.initiate(this, stack, level, world, player, source, cooldown, slot);
        if (result != null) ci.setReturnValue(result);
    }
}
