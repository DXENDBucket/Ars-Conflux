package dev.arsconflux.platform;

import dev.arsconflux.ArsConflux;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TimingAttributes {
    private static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create(Registries.ATTRIBUTE, ArsConflux.MOD_ID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<Attribute, Attribute> COOLDOWN_REDUCTION =
            REGISTRY.register("cooldown_reduction", () -> new RangedAttribute("attribute.ars_conflux.cooldown_reduction", 0, -10, 1).setSyncable(true));
    public static final net.neoforged.neoforge.registries.DeferredHolder<Attribute, Attribute> CAST_TIME_REDUCTION =
            REGISTRY.register("cast_time_reduction", () -> new RangedAttribute("attribute.ars_conflux.cast_time_reduction", 0, -10, 1).setSyncable(true));
    private TimingAttributes() {}
    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
        bus.addListener((EntityAttributeModificationEvent event) -> {
            for (var type : event.getTypes()) {
                event.add(type, COOLDOWN_REDUCTION);
                event.add(type, CAST_TIME_REDUCTION);
            }
        });
    }
    public static double cooldownReduction(LivingEntity entity) {
        var instance = entity == null ? null : entity.getAttribute(COOLDOWN_REDUCTION);
        return instance == null ? 0 : instance.getValue();
    }
    public static double castTimeReduction(LivingEntity entity) {
        var instance = entity == null ? null : entity.getAttribute(CAST_TIME_REDUCTION);
        return instance == null ? 0 : instance.getValue();
    }
}
