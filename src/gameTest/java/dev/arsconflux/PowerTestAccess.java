package dev.arsconflux;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
public final class PowerTestAccess {
 public static void general(LivingEntity entity,double value) {entity.getAttribute(dev.arsconflux.platform.PowerAttributes.CONFLUENCE).setBaseValue(value);}
 public static void school(LivingEntity entity,ResourceLocation id,double value) {entity.getAttribute(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(dev.arsconflux.api.damage.SpellPower.attributeId(id)).orElseThrow()).setBaseValue(value);}
}
