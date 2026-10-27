package dev.arsconflux.mixin;

import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.internal.zero.ZeroTemporal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.glyph.AnchorEffect",remap=false)
public abstract class OptionalZeroAnchorMixin {
    @WrapMethod(method="onResolveEntity")
    private void arsConflux$entity(EntityHitResult hit,Level level,LivingEntity caster,SpellStats stats,SpellContext context,SpellResolver resolver,Operation<Void> original) {
        ZeroTemporal.anchor(context,()->original.call(hit,level,caster,stats,context,resolver));
    }
    @WrapMethod(method="onResolveBlock")
    private void arsConflux$block(BlockHitResult hit,Level level,LivingEntity caster,SpellStats stats,SpellContext context,SpellResolver resolver,Operation<Void> original) {
        ZeroTemporal.anchor(context,()->original.call(hit,level,caster,stats,context,resolver));
    }
    @WrapMethod(method="restoreEntityPhysics")
    private static void arsConflux$restore(MultiPhaseCastContext context,Operation<Void> original) {
        var touched=context==null?java.util.List.<com.github.ars_zero.common.spell.SpellResult>of():java.util.List.copyOf(ZeroTemporal.physicsResults(context));
        try { ZeroTemporal.restore(context,c->original.call(c)); }
        finally {
            for(var result:touched)if(result!=null && result.targetEntity!=null)
                dev.arsconflux.api.projectile.ProjectileControls.release(result.targetEntity,context);
        }
    }
}
