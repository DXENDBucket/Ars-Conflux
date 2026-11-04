package dev.arsconflux.mixin.client;

import java.util.ServiceLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Use the service interface's mod class loader, independent of the loading worker's context loader. */
@Pseudo
@Mixin(targets = "at.petrak.hexcasting.xplat.IClientXplatAbstractions", remap = false)
public interface OptionalHexClientServicesMixin {
    @Redirect(method = "find()Lat/petrak/hexcasting/xplat/IClientXplatAbstractions;",
            at = @At(value = "INVOKE", target = "Ljava/util/ServiceLoader;load(Ljava/lang/Class;)Ljava/util/ServiceLoader;"))
    private static <T> ServiceLoader<T> arsConflux$clientServices(Class<T> service) {
        return ServiceLoader.load(service, service.getClassLoader());
    }
}
