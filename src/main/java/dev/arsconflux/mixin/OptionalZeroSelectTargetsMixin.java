package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.internal.zero.ZeroTemporal;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.glyph.SelectEffect",remap=false)
public abstract class OptionalZeroSelectTargetsMixin {
    @WrapMethod(method="createBlockGroup")
    private void arsConflux$group(ServerLevel level,List<BlockPos> positions,Player player,SpellContext context,LivingEntity caster,Operation<Void> original) {
        ZeroTemporal.produced(context,()->original.call(level,positions,player,context,caster));
    }
}
