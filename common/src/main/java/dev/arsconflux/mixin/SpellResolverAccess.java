package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = SpellResolver.class, remap = false)
public interface SpellResolverAccess {
    @Invoker("enoughMana") boolean arsConflux$enoughMana(LivingEntity caster);
}
