package dev.arsconflux.internal.content;
import com.mojang.serialization.MapCodec;
import dev.arsconflux.config.ConfluxStartupConfig;
import net.neoforged.neoforge.common.conditions.ICondition;
public record NearbyAlliesCondition() implements ICondition {
    public static final MapCodec<NearbyAlliesCondition> CODEC=MapCodec.unit(new NearbyAlliesCondition());
    public boolean test(IContext context) {return ConfluxStartupConfig.nearbyAllies();}
    public MapCodec<? extends ICondition> codec() {return CODEC;}
}
