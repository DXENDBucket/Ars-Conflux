package dev.arsconflux.mixin.client;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public abstract class SchoolPowerLanguageMixin {
    @Inject(method="getOrDefault",at=@At("RETURN"),cancellable=true)
    private void arsConflux$schoolPowerName(String key,String fallback,CallbackInfoReturnable<String> ci) {
        String prefix="attribute.ars_conflux.school_power.";
        if(!key.startsWith(prefix)||!ci.getReturnValue().equals(fallback))return;
        var label=key.substring(prefix.length());
        var language=(ClientLanguage)(Object)this;
        ci.setReturnValue(language.getOrDefault("attribute.ars_conflux.school_power_format","%s School Spell Power").replace("%s",label));
    }
}
