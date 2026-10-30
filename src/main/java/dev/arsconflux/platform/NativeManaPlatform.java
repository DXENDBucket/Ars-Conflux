package dev.arsconflux.platform;

import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.event.*;
import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.common.capability.ManaCap;
import com.hollingsworth.arsnouveau.setup.registry.*;
import dev.arsconflux.api.resource.MobMana;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class NativeManaPlatform {
    private static final ResourceLocation MAX=ResourceLocation.parse("ars_conflux:mob_base_mana"),REGEN=ResourceLocation.parse("ars_conflux:mob_base_regeneration");
    private static final class ManagedCap extends ManaCap {ManagedCap(LivingEntity actor){super(actor);}}
    public static void register(IEventBus bus) {
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->
                dev.arsconflux.internal.billing.ManaAttributeLimits.apply(PerkAttributes.MAX_MANA.value(),PerkAttributes.MANA_REGEN_BONUS.value())));
        bus.addListener((EntityAttributeModificationEvent event)->{
            for(var type:event.getTypes())if(type!=EntityType.PLAYER){
                if(!event.has(type,PerkAttributes.MAX_MANA))event.add(type,PerkAttributes.MAX_MANA);
                if(!event.has(type,PerkAttributes.MANA_REGEN_BONUS))event.add(type,PerkAttributes.MANA_REGEN_BONUS);
            }
        });
        bus.addListener(EventPriority.LOWEST,(RegisterCapabilitiesEvent event)->{
            for(var type:BuiltInRegistries.ENTITY_TYPE)if(type!=EntityType.PLAYER)
                event.registerEntity(CapabilityRegistry.MANA_CAPABILITY,type,(entity,context)->{
                    if(!(entity instanceof LivingEntity actor)||entity.level().isClientSide)return null;
                    boolean existing=actor.hasData(AttachmentsRegistry.MANA_ATTACHMENT);
                    var cap=new ManagedCap(actor);MobMana.initialize(actor,cap,existing);return cap;
                });
        });
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post event)->{if(event.getEntity() instanceof LivingEntity actor)MobMana.tick(actor);});
    }
    public static IManaCap get(LivingEntity actor){return CapabilityRegistry.getMana(actor);}
    public static boolean owns(IManaCap cap){return cap instanceof ManagedCap;}
    private static double attribute(LivingEntity actor,Holder<Attribute> type,ResourceLocation id,double base) {
        var attr=actor.getAttribute(type);if(attr==null)return base;
        var old=attr.getModifier(id);if(old==null||old.amount()!=base){attr.removeModifier(id);attr.addTransientModifier(new AttributeModifier(id,base,AttributeModifier.Operation.ADD_VALUE));}
        return attr.getValue();
    }
    public static int maximum(LivingEntity actor,IManaCap cap,double base) {
        var event=new MaxManaCalcEvent(actor,(int)Math.min(Integer.MAX_VALUE,Math.max(0,attribute(actor,PerkAttributes.MAX_MANA,MAX,base))));NeoForge.EVENT_BUS.post(event);
        float reserve=Float.isFinite(event.getReserve())?Math.clamp(event.getReserve(),0f,1f):0;
        if(cap instanceof ManaCap mana)mana.setReserve(reserve);
        return (int)Math.max(0,Math.min(Integer.MAX_VALUE,event.getMax()*(1.0-reserve)));
    }
    public static double regeneration(LivingEntity actor,double base) {
        var event=new ManaRegenCalcEvent(actor,attribute(actor,PerkAttributes.MANA_REGEN_BONUS,REGEN,base));NeoForge.EVENT_BUS.post(event);return event.getRegen();
    }
    public static double playerRegen(Player player){return com.hollingsworth.arsnouveau.api.util.ManaUtil.getManaRegen(player);}
    public static void sync(LivingEntity actor,IManaCap cap){if(actor instanceof ServerPlayer p&&cap instanceof ManaCap mana)mana.syncToClient(p);}
    public static boolean isProxy(Entity source,LivingEntity caster){return source instanceof net.neoforged.neoforge.common.util.FakePlayer&&source.getUUID().equals(caster.getUUID());}
    private NativeManaPlatform() {}
}
