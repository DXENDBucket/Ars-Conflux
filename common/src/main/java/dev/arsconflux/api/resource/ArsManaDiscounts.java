package dev.arsconflux.api.resource;

import top.theillusivec4.curios.api.CuriosApi;
import dev.arsconflux.config.ConfluxConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Native-ring percentage policy, independent of resource balances, reservations and native soul rules. */
public final class ArsManaDiscounts {
    public static boolean nativeRing(ItemStack stack) {
        if(stack==null||stack.isEmpty())return false;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.toString().equals("ars_nouveau:ring_of_lesser_discount")||id.toString().equals("ars_nouveau:ring_of_greater_discount");
    }
    private static BigDecimal ringRate(ItemStack stack) {
        if(!nativeRing(stack))return BigDecimal.ZERO;
        return BigDecimal.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("ring_of_lesser_discount")
                ?ConfluxConfig.lesserRingReduction():ConfluxConfig.greaterRingReduction());
    }
    private static BigDecimal reduction(LivingEntity caster,ItemStack tool) {
        if(!ConfluxConfig.percentageDiscountRings()||caster==null)return BigDecimal.ZERO;
        var total=BigDecimal.ZERO;var curios=CuriosApi.getCuriosInventory(caster).map(inventory -> inventory.getEquippedCurios()).orElse(null);
        if(curios!=null)for(int i=0;i<curios.getSlots();i++)total=total.add(ringRate(curios.getStackInSlot(i)));
        for(var stack:caster.getArmorSlots())total=total.add(ringRate(stack));
        return total.add(ringRate(tool)).min(BigDecimal.ONE);
    }
    public static double reduction(LivingEntity caster) {return reduction(caster,ItemStack.EMPTY).doubleValue();}
    public static int quote(LivingEntity caster,int raw) {return apply(raw,reduction(caster,ItemStack.EMPTY));}
    public static int apply(int raw,BigDecimal reduction) {
        if(raw<0)throw new IllegalArgumentException("Negative mana cost");
        var fraction=reduction.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return BigDecimal.valueOf(raw).multiply(BigDecimal.ONE.subtract(fraction)).setScale(0,RoundingMode.CEILING).intValueExact();
    }
    /** Reuse native cost/event handling: remove rings' old flat part, then return equivalent percentage credit. */
    public static int nativeDiscount(LivingEntity caster,ItemStack tool,int raw,int otherDiscount) {
        if(!ConfluxConfig.percentageDiscountRings())return otherDiscount;
        int remainder=(int)Math.max(0,Math.min(Integer.MAX_VALUE,(long)raw-otherDiscount));
        long extra=(long)remainder-apply(remainder,reduction(caster,tool));
        return (int)Math.max(Integer.MIN_VALUE,Math.min(Integer.MAX_VALUE,(long)otherDiscount+extra));
    }
    public static String ringPercent(ItemStack stack) {return ringRate(stack).multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();}
    private ArsManaDiscounts() {}
}
