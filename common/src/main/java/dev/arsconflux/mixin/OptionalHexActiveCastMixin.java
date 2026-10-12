package dev.arsconflux.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.internal.hex.HexLinkBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = {"io.yukkuric.hex_ars_link.action.OpTouchCast$Action", "io.yukkuric.hex_ars_link.action.OpShootCast$Action"}, remap = false)
public abstract class OptionalHexActiveCastMixin {
    @WrapMethod(method = "cast(Lat/petrak/hexcasting/api/casting/eval/CastingEnvironment;)V")
    private void arsConflux$active(@Coerce Object environment, Operation<Void> original) {
        HexLinkBridge.active(this, () -> original.call(environment));
    }
}
