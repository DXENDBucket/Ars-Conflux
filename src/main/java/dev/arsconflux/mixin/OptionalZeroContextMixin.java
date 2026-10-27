package dev.arsconflux.mixin;

import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import dev.arsconflux.internal.zero.*;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.spell.MultiPhaseCastContext",remap=false)
public abstract class OptionalZeroContextMixin implements ZeroTemporalOwner {
    @Unique private ZeroTemporalState arsConflux$state;
    public ZeroTemporalState arsConflux$temporal() {
        if(arsConflux$state==null)arsConflux$state=new ZeroTemporalState((MultiPhaseCastContext)(Object)this);
        return arsConflux$state;
    }
}
