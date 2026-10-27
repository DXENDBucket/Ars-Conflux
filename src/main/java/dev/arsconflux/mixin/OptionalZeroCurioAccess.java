package dev.arsconflux.mixin;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.github.ars_zero.event.CurioCastingHandler", remap = false)
public interface OptionalZeroCurioAccess {
    @Accessor("ACTIVE_CASTERS") static Set<UUID> arsConflux$active() { throw new AssertionError(); }
    @Invoker("findCirclet") static Optional<ItemStack> arsConflux$circlet(Player player) { throw new AssertionError(); }
}
