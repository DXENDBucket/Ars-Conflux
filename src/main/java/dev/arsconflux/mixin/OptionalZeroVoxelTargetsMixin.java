package dev.arsconflux.mixin;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.internal.zero.ZeroTemporal;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.glyph.ConjureVoxelEffect",remap=false)
public abstract class OptionalZeroVoxelTargetsMixin {
    @WrapMethod(method="updateTemporalContext")
    private void arsConflux$one(LivingEntity caster,BaseVoxelEntity voxel,SpellContext context,Operation<Void> original) {
        ZeroTemporal.produced(context,()->original.call(caster,voxel,context));
    }
    @WrapMethod(method="updateTemporalContextMultiple")
    private void arsConflux$many(LivingEntity caster,List<BaseVoxelEntity> voxels,SpellContext context,Operation<Void> original) {
        ZeroTemporal.produced(context,()->original.call(caster,voxels,context));
    }
}
