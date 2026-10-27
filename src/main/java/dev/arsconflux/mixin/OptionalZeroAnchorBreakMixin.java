package dev.arsconflux.mixin;

import com.github.ars_zero.common.spell.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.arsconflux.internal.zero.ZeroTemporal;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.event.AnchorEffectEvents",remap=false)
public abstract class OptionalZeroAnchorBreakMixin {
    @WrapOperation(method="breakAnchorsTargeting",at=@At(value="FIELD",target="Lcom/github/ars_zero/common/spell/MultiPhaseCastContext;beginResults:Ljava/util/List;",opcode=Opcodes.GETFIELD))
    private static List<SpellResult> arsConflux$anchored(MultiPhaseCastContext context,Operation<List<SpellResult>> original) {
        return ZeroTemporal.physicsResults(context);
    }
}
