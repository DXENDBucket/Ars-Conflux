package dev.arsconflux.platform;

import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.event.*;
import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.common.capability.ManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.arsconflux.api.resource.MobMana;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;
import java.util.UUID;

public final class NativeManaPlatform {
    private static final UUID MAX=UUID.fromString("c7bb53b6-8f57-4f1c-af88-7b235d840be1"),REGEN=UUID.fromString("c7bb53b6-8f57-4f1c-af88-7b235d840be2");
    private static final class ManagedCap extends ManaCap {ManagedCap(LivingEntity actor){super(actor);}}
    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final LivingEntity actor;
        private final ManagedCap cap;
        private boolean restored;
        private final LazyOptional<IManaCap> value;
        Provider(LivingEntity actor){this.actor=actor;cap=new ManagedCap(actor);value=LazyOptional.of(()->cap);}
        @Override public <T> LazyOptional<T> getCapability(Capability<T> requested,Direction side){if(requested!=CapabilityRegistry.MANA_CAPABILITY)return LazyOptional.empty();MobMana.initialize(actor,cap,restored);return value.cast();}
        @Override public CompoundTag serializeNBT(){return actor.getPersistentData().getBoolean(MobMana.ACTIVE)?cap.serializeNBT():new CompoundTag();}
        @Override public void deserializeNBT(CompoundTag data){restored=!data.isEmpty();if(restored)cap.deserializeNBT(data);}
    }
    public static void register(IEventBus bus) {
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->
                dev.arsconflux.internal.billing.ManaAttributeLimits.apply(PerkAttributes.MAX_MANA.get(),PerkAttributes.MANA_REGEN_BONUS.get())));
        bus.addListener((EntityAttributeModificationEvent event)->{
            for(var type:event.getTypes())if(type!=EntityType.PLAYER){
                if(!event.has(type,PerkAttributes.MAX_MANA.get()))event.add(type,PerkAttributes.MAX_MANA.get());
                if(!event.has(type,PerkAttributes.MANA_REGEN_BONUS.get()))event.add(type,PerkAttributes.MANA_REGEN_BONUS.get());
            }
        });
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class,EventPriority.LOWEST,(AttachCapabilitiesEvent<Entity> event)->{
            if(event.getObject() instanceof LivingEntity actor&&!(actor instanceof Player)&&!actor.level().isClientSide){
                var provider=new Provider(actor);event.addCapability(new ResourceLocation("ars_conflux","mob_mana"),provider);event.addListener(provider.value::invalidate);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event)->MobMana.tick(event.getEntity()));
    }
    public static IManaCap get(LivingEntity actor){return CapabilityRegistry.getMana(actor).resolve().orElse(null);}
    public static boolean owns(IManaCap cap){return cap instanceof ManagedCap;}
    private static double attribute(LivingEntity actor,Attribute type,UUID id,double base) {
        var attr=actor.getAttribute(type);if(attr==null)return base;
        var old=attr.getModifier(id);if(old==null||old.getAmount()!=base){attr.removeModifier(id);attr.addTransientModifier(new AttributeModifier(id,"Conflux native mob mana",base,AttributeModifier.Operation.ADDITION));}
        return attr.getValue();
    }
    public static int maximum(LivingEntity actor,IManaCap cap,double base) {
        var event=new MaxManaCalcEvent(actor,(int)Math.min(Integer.MAX_VALUE,Math.max(0,attribute(actor,PerkAttributes.MAX_MANA.get(),MAX,base))));MinecraftForge.EVENT_BUS.post(event);
        float reserve=Float.isFinite(event.getReserve())?Math.max(0,Math.min(1,event.getReserve())):0;
        return (int)Math.max(0,Math.min(Integer.MAX_VALUE,event.getMax()*(1.0-reserve)));
    }
    public static double regeneration(LivingEntity actor,double base) {
        var event=new ManaRegenCalcEvent(actor,attribute(actor,PerkAttributes.MANA_REGEN_BONUS.get(),REGEN,base));MinecraftForge.EVENT_BUS.post(event);return event.getRegen();
    }
    public static double playerRegen(Player player){return com.hollingsworth.arsnouveau.api.util.ManaUtil.getManaRegen(player);}
    public static void sync(LivingEntity actor,IManaCap cap){/* Native Ars player's tick performs its existing synchronization. */}
    public static boolean isProxy(Entity source,LivingEntity caster){return source instanceof com.hollingsworth.arsnouveau.api.ANFakePlayer;}
    private NativeManaPlatform() {}
}
