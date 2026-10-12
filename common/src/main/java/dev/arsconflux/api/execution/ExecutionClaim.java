package dev.arsconflux.api.execution;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;

/** A session owns a claim on its actual actor. Different channels are independent. */
public record ExecutionClaim(LivingEntity actor,ResourceLocation channel,boolean exclusive) {
    public static final ResourceLocation ENTITY_ACTION=ResourceLocation.tryParse("ars_conflux:entity_action");
    public static final ResourceLocation CHANNELING=ResourceLocation.tryParse("ars_conflux:channeling");
    public ExecutionClaim {Objects.requireNonNull(actor);Objects.requireNonNull(channel);}
    public static ExecutionClaim action(LivingEntity actor){return new ExecutionClaim(actor,ENTITY_ACTION,true);}
    /** Shared channels may coexist; callers can declare an exclusive channel when needed. */
    public static ExecutionClaim channel(LivingEntity actor){return new ExecutionClaim(actor,CHANNELING,false);}
    public boolean conflicts(ExecutionClaim other){return actor==other.actor && channel.equals(other.channel) && (exclusive || other.exclusive);}
}
