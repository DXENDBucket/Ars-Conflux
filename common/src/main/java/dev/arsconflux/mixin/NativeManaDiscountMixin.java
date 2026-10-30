package dev.arsconflux.mixin;
import com.hollingsworth.arsnouveau.api.util.ManaUtil;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.resource.ArsManaDiscounts;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ManaUtil.class,remap=false)
public abstract class NativeManaDiscountMixin {
    @Inject(method="getPlayerDiscounts",at=@At("RETURN"),cancellable=true)
    private static void arsConflux$nativePercentage(LivingEntity caster,Spell spell,ItemStack tool,CallbackInfoReturnable<Integer> ci) {
        if(caster==null||spell==null)return;
        ci.setReturnValue(ArsManaDiscounts.nativeDiscount(caster,tool,spell.getCost(),ci.getReturnValueI()));
    }
}
