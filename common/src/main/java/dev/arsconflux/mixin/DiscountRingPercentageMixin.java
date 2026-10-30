package dev.arsconflux.mixin;
import com.hollingsworth.arsnouveau.common.items.curios.DiscountRing;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.resource.ArsManaDiscounts;
import dev.arsconflux.config.ConfluxConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=DiscountRing.class,remap=false)
public abstract class DiscountRingPercentageMixin {
    @Inject(method="getManaDiscount(Lnet/minecraft/world/item/ItemStack;Lcom/hollingsworth/arsnouveau/api/spell/Spell;)I",at=@At("HEAD"),cancellable=true)
    private void arsConflux$removeFlat(ItemStack stack,Spell spell,CallbackInfoReturnable<Integer> ci) {
        if(ConfluxConfig.percentageDiscountRings()&&ArsManaDiscounts.nativeRing(stack))ci.setReturnValue(0);
    }
    @WrapOperation(method="appendHoverText",remap=true,at=@At(value="INVOKE",target="Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;",remap=true))
    private MutableComponent arsConflux$percentTooltip(String key,Object[] args,Operation<MutableComponent> original) {
        var stack=new ItemStack((DiscountRing)(Object)this);
        return ConfluxConfig.percentageDiscountRings()&&ArsManaDiscounts.nativeRing(stack)
                ?original.call("ars_conflux.tooltip.percentage_mana_discount",new Object[]{ArsManaDiscounts.ringPercent(stack)})
                :original.call(key,args);
    }
}
