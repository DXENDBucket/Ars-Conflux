package dev.arsconflux.mixin;

import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellPhase;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.internal.zero.ZeroChannelCasts;
import dev.arsconflux.internal.zero.ZeroTemporal;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import net.minecraft.world.entity.LivingEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice",remap=false)
public abstract class OptionalZeroDeviceMixin {
    @WrapOperation(method="checkManaAndCast",at=@At(value="INVOKE",target="Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;canCast(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean arsConflux$execution(SpellResolver resolver,LivingEntity caster,Operation<Boolean> original,@Local(argsOnly=true) SpellPhase phase) {
        boolean allowed=original.call(resolver,caster);
        if(allowed)ZeroTemporal.begin(resolver,phase);
        return allowed;
    }
    @WrapMethod(method="beginPhase")
    private void arsConflux$begin(Player p,ItemStack stack,MultiPhaseCastContext.CastSource source,Operation<Void> original) {
        ZeroChannelCasts.begin(p,stack,source,()->original.call(p,stack,source));
    }
    @WrapMethod(method="checkManaAndCast")
    private boolean arsConflux$phase(Player p,ItemStack stack,Spell spell,SpellPhase phase,Operation<Boolean> original) {
        return ZeroChannelCasts.phase(p,stack,spell,phase,()->original.call(p,stack,spell,phase));
    }
    @WrapMethod(method="endPhase")
    private void arsConflux$end(Player p,ItemStack stack,Operation<Void> original) {
        ZeroChannelCasts.end(p,stack,()->original.call(p,stack));
    }
}
