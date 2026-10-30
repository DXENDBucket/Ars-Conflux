package dev.arsconflux.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.damage.SpellPower;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLevel.class)
public abstract class SpellPowerTickMixin {
    @WrapMethod(method="tickNonPassenger")
    private void arsConflux$powerTick(Entity entity,Operation<Void> original) {
        SpellPower.tick(entity,()->original.call(entity));
    }
    @WrapMethod(method="tickPassenger")
    private void arsConflux$powerPassenger(Entity vehicle,Entity passenger,Operation<Void> original) {
        SpellPower.tick(passenger,()->original.call(vehicle,passenger));
    }
}
