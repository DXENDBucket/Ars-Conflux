package dev.arsconflux.mixin;

import dev.arsconflux.internal.zero.ZeroBlockSafety;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.util.BlockImmutabilityUtil",remap=false)
public abstract class OptionalZeroImmutableMixin {
    @Inject(method="isBlockImmutable(Lnet/minecraft/world/level/block/Block;)Z",at=@At("HEAD"),cancellable=true)
    private static void arsConflux$tag(Block block,CallbackInfoReturnable<Boolean> ci) {
        if(ZeroBlockSafety.immutable(block))ci.setReturnValue(true);
    }
}
