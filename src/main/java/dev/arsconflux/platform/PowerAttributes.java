package dev.arsconflux.platform;

import dev.arsconflux.ArsConflux;
import dev.arsconflux.config.ConfluxStartupConfig;
import dev.arsconflux.api.damage.SpellPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Registry values are fixed at startup; school definitions and assignments remain reloadable. */
public final class PowerAttributes {
    private static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create(Registries.ATTRIBUTE, ArsConflux.MOD_ID);
    public static final DeferredHolder<Attribute, Attribute> CONFLUENCE = REGISTRY.register("confluence_spell_power",
            () -> attribute("attribute.ars_conflux.confluence_spell_power"));
    private static final Map<ResourceLocation, DeferredHolder<Attribute, Attribute>> SCHOOLS = new LinkedHashMap<>();
    static {
        for (String text : new TreeSet<>(ConfluxStartupConfig.schoolPowerAttributes())) {
            var school=ResourceLocation.tryParse(text);
            if(school==null)continue;
            var id=SpellPower.attributeId(school);
            SCHOOLS.put(school,REGISTRY.register(id.getPath(),()->attribute("attribute.ars_conflux.school_power."+school)));
        }
    }
    private static Attribute attribute(String key) {
        return new RangedAttribute(key,0,-1024,1024) {
            @Override public net.minecraft.network.chat.MutableComponent toValueComponent(
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation,double value,net.minecraft.world.item.TooltipFlag flag) {
                return net.minecraft.network.chat.Component.literal(net.neoforged.neoforge.common.extensions.IAttributeExtension.FORMAT.format(value*100)+"%");
            }
        }.setSyncable(true);
    }
    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
        bus.addListener((EntityAttributeModificationEvent event)->{
            for(var type:event.getTypes()) {
                event.add(type,CONFLUENCE);
                for(var attribute:SCHOOLS.values())event.add(type,attribute);
            }
        });
    }
    private static double value(LivingEntity caster,DeferredHolder<Attribute, Attribute> attribute) {
        if(caster==null||attribute==null)return 0;
        var instance=caster.getAttribute(attribute);return instance==null?0:instance.getValue();
    }
    public static double general(LivingEntity caster) {return value(caster,CONFLUENCE);}
    public static double school(LivingEntity caster,ResourceLocation id) {return value(caster,SCHOOLS.get(id));}
    public static Set<ResourceLocation> registeredSchools() {return Collections.unmodifiableSet(SCHOOLS.keySet());}
    private PowerAttributes() {}
}
