package dev.arsconflux.api.casting;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;
public record ActiveCastRequest(Spell spell, LivingEntity caster, ActiveCastSource source) {
    public ActiveCastRequest { Objects.requireNonNull(spell); Objects.requireNonNull(caster); Objects.requireNonNull(source); }
}
