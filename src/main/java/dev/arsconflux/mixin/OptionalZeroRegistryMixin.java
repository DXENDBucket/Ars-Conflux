package dev.arsconflux.mixin;

import com.github.ars_zero.common.spell.*;
import dev.arsconflux.internal.zero.ZeroTemporal;
import java.util.UUID;
import java.util.Map;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.spell.MultiPhaseCastContextRegistry",remap=false)
public abstract class OptionalZeroRegistryMixin {
    @Shadow @Final private static Map<UUID,MultiPhaseCastContext> contexts;
    @Inject(method="remove",at=@At("HEAD"))
    private static void arsConflux$cleanup(UUID id,CallbackInfo ci) {
        var context=MultiPhaseCastContextRegistry.get(id);
        if(context!=null)ZeroTemporal.close(context);
    }
    @Inject(method="clear",at=@At("HEAD"))
    private static void arsConflux$clear(CallbackInfo ci) {
        for(var context:java.util.List.copyOf(contexts.values()))ZeroTemporal.close(context);
    }
}
