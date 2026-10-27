package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.arsconflux.internal.zero.ZeroBlockSafety;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="com.github.ars_zero.event.ArsZeroResolverEvents",remap=false)
public abstract class OptionalZeroBlockSafetyMixin {
    @WrapOperation(method="onEffectResolved",at=@At(value="INVOKE",target="Lcom/github/ars_zero/common/util/BlockImmutabilityUtil;isBlockImmutable(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean arsConflux$postCapture(BlockState state,Operation<Boolean> original,@Local(argsOnly=true) EffectResolveEvent.Post event) {
        // Post may fall back to still-present world states rejected by Pre; reject them here too.
        return original.call(state) || !ZeroBlockSafety.movable(event.world,BlockPos.containing(event.rayTraceResult.getLocation()),state);
    }
    @WrapOperation(method="onEffectResolving",at=@At(value="INVOKE",target="Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object arsConflux$capture(Map<Object,Object> map,Object key,Object value,Operation<Object> original,
            @Local(argsOnly=true) EffectResolveEvent.Pre event) {
        return key instanceof BlockPos pos && value instanceof BlockState state && !ZeroBlockSafety.movable(event.world,pos,state)
                ? null : original.call(map,key,value);
    }
    @WrapOperation(method="onEffectResolving",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean arsConflux$remove(Level level,BlockPos pos,BlockState state,int flags,Operation<Boolean> original) {
        return ZeroBlockSafety.movable(level,pos,level.getBlockState(pos)) && original.call(level,pos,state,flags);
    }
}
