package dev.arsconflux.platform;

import dev.arsconflux.ArsConflux;
import dev.arsconflux.config.ConfluxStartupConfig;
import dev.arsconflux.api.damage.SpellPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import java.util.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registry values are fixed at startup; school definitions and assignments remain reloadable. */
public final class PowerAttributes {
    private static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ArsConflux.MOD_ID);
    public static final RegistryObject<Attribute> CONFLUENCE = REGISTRY.register("confluence_spell_power",
            () -> attribute("attribute.ars_conflux.confluence_spell_power"));
    private static final Map<ResourceLocation, RegistryObject<Attribute>> SCHOOLS = new LinkedHashMap<>();
    static {
        for (String text : new TreeSet<>(ConfluxStartupConfig.schoolPowerAttributes())) {
            var school=ResourceLocation.tryParse(text);
            if(school==null)continue;
            var id=SpellPower.attributeId(school);
            SCHOOLS.put(school,REGISTRY.register(id.getPath(),()->attribute("attribute.ars_conflux.school_power."+school)));
        }
    }
    private static Attribute attribute(String key) { return new RangedAttribute(key,0,-1024,1024).setSyncable(true); }
    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.ItemTooltipEvent event)->{
            var lines=event.getToolTip();
            for(int i=0;i<lines.size();i++) {
                var line=lines.get(i);
                if(!(line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text)
                        ||!java.util.Set.of("attribute.modifier.plus.0","attribute.modifier.take.0").contains(text.getKey()))continue;
                var args=text.getArgs();
                if(args.length<2||!(args[1] instanceof net.minecraft.network.chat.Component name)
                        ||!(name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents label)
                        ||!(label.getKey().equals("attribute.ars_conflux.confluence_spell_power")||label.getKey().startsWith("attribute.ars_conflux.school_power.")))continue;
                try {
                    var percent=net.minecraft.world.item.ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(Double.parseDouble(args[0].toString())*100)+"%";
                    lines.set(i,net.minecraft.network.chat.Component.translatable(text.getKey(),percent,args[1]).withStyle(line.getStyle()));
                }catch(NumberFormatException ignored){}
            }
        });
        bus.addListener((EntityAttributeModificationEvent event)->{
            for(var type:event.getTypes()) {
                event.add(type,CONFLUENCE.get());
                for(var attribute:SCHOOLS.values())event.add(type,attribute.get());
            }
        });
    }
    private static double value(LivingEntity caster,RegistryObject<Attribute> attribute) {
        if(caster==null||attribute==null)return 0;
        var instance=caster.getAttribute(attribute.get());return instance==null?0:instance.getValue();
    }
    public static double general(LivingEntity caster) {return value(caster,CONFLUENCE);}
    public static double school(LivingEntity caster,ResourceLocation id) {return value(caster,SCHOOLS.get(id));}
    public static Set<ResourceLocation> registeredSchools() {return Collections.unmodifiableSet(SCHOOLS.keySet());}
    private PowerAttributes() {}
}
