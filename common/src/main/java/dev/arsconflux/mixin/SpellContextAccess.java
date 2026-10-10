package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reading stored context fields must not ask Ars to construct a server-only fake player. */
@Mixin(value = SpellContext.class, remap = false)
public interface SpellContextAccess {
    @Accessor("level") Level arsConflux$level();
    @Accessor("caster") LivingEntity arsConflux$caster();
}
