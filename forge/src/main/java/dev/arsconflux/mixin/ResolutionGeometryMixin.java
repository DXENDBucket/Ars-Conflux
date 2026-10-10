package dev.arsconflux.mixin;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.api.context.TriggerGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = SpellResolver.class, remap = false)
public abstract class ResolutionGeometryMixin {
    @Inject(method = "onResolveEffect", at = @At("HEAD"))
    private void arsConflux$snapshot(CallbackInfo ci) { TriggerGeometry.capture(((SpellResolver)(Object)this).spellContext); }
}
