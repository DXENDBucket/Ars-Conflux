package dev.arsconflux.mixin;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import dev.arsconflux.api.projectile.ProjectileForm;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=AbstractAugment.class,remap=false)
public abstract class ProjectileGlyphTypeMixin {
    @Inject(method="getTypeName",at=@At("HEAD"),cancellable=true)
    private void conflux$type(CallbackInfoReturnable<Component> cir){
        if(this instanceof ProjectileForm)cir.setReturnValue(Component.translatable("ars_conflux.glyph_type.projectile"));
    }
}
