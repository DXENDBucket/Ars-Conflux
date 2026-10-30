package dev.arsconflux.api.resource;

import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.setup.config.ServerConfig;
import dev.arsconflux.platform.NativeManaPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.*;
import java.util.function.Function;

/** Lifecycle policy for the native Ars pool; no parallel resource store or species knowledge. */
public final class MobMana {
    public static final String ACTIVE="ars_conflux_mana_active";
    private static final String PROFILE="ars_conflux_mana_profile";
    public record Profile(double maximum,double regeneration) {
        public Profile {if(!Double.isFinite(maximum)||maximum<0||maximum>Integer.MAX_VALUE
                ||!Double.isFinite(regeneration)||regeneration<0)throw new IllegalArgumentException("Invalid mob mana profile");}
    }
    private record Policy(ResourceLocation id,int priority,Function<LivingEntity,Profile> lookup) {}
    private static volatile List<Policy> policies=List.of();
    public static synchronized void registerPolicy(ResourceLocation id,int priority,Function<LivingEntity,Profile> lookup) {
        Objects.requireNonNull(id);Objects.requireNonNull(lookup);
        if(policies.stream().anyMatch(p->p.id().equals(id)))throw new IllegalArgumentException("Duplicate mana policy: "+id);
        var next=new ArrayList<>(policies);next.add(new Policy(id,priority,lookup));
        next.sort(Comparator.comparingInt(Policy::priority).reversed().thenComparing(p->p.id().toString()));policies=List.copyOf(next);
    }
    public static Profile profile(LivingEntity actor) {
        var data=actor.getPersistentData();
        if(data.contains(PROFILE)) {var p=data.getCompound(PROFILE);return new Profile(p.getDouble("maximum"),p.getDouble("regeneration"));}
        for(var policy:policies){var value=policy.lookup().apply(actor);if(value!=null)return value;}
        return new Profile(ServerConfig.INIT_MAX_MANA.get(),ServerConfig.INIT_MANA_REGEN.get());
    }
    /** Explicit per-entity override. Changing an existing capacity clamps, never refills. */
    public static void configure(LivingEntity actor,Profile profile) {
        if(actor instanceof Player||actor.level().isClientSide)throw new IllegalArgumentException("Server non-player only");
        var data=new net.minecraft.nbt.CompoundTag();data.putDouble("maximum",profile.maximum());data.putDouble("regeneration",profile.regeneration());
        actor.getPersistentData().put(PROFILE,data);
        var cap=NativeManaPlatform.get(actor);if(cap!=null&&NativeManaPlatform.owns(cap))refresh(actor,cap);
    }
    /** Platform fallback providers call this only when their native-backed capability is selected. */
    public static void initialize(LivingEntity actor,IManaCap cap,boolean existing) {
        if(actor instanceof Player||actor.level().isClientSide||actor.getPersistentData().getBoolean(ACTIVE))return;
        actor.getPersistentData().putBoolean(ACTIVE,true);refresh(actor,cap);
        if(!existing)cap.setMana(cap.getMaxMana());
    }
    public static void refresh(LivingEntity actor,IManaCap cap) {
        double base=profile(actor).maximum()+cap.getGlyphBonus()*(double)ServerConfig.GLYPH_MAX_BONUS.get()
                +cap.getBookTier()*(double)ServerConfig.TIER_MAX_BONUS.get();
        int maximum=NativeManaPlatform.maximum(actor,cap,base);
        cap.setMaxMana(maximum);cap.setMana(cap.getCurrentMana());
    }
    public static double regeneration(LivingEntity actor,IManaCap cap) {
        double base=profile(actor).regeneration()+cap.getGlyphBonus()*ServerConfig.GLYPH_REGEN_BONUS.get()
                +cap.getBookTier()*(double)ServerConfig.TIER_REGEN_BONUS.get();
        double result=NativeManaPlatform.regeneration(actor,base);
        return Double.isFinite(result)?Math.max(0,result):0;
    }
    /** Called by the platform entity tick; unactivated actors only pay a few scalar checks. */
    public static void tick(LivingEntity actor) {
        if(actor instanceof Player||actor.level().isClientSide||!actor.isAlive()||(actor.tickCount+actor.getId())%20!=0
                ||!actor.getPersistentData().getBoolean(ACTIVE))return;
        var cap=NativeManaPlatform.get(actor);
        if(cap==null||!NativeManaPlatform.owns(cap))return; // A foreign provider owns its own lifecycle.
        refresh(actor,cap);if(cap.getCurrentMana()<cap.getMaxMana())cap.addMana(regeneration(actor,cap));
    }
    private MobMana() {}
}
