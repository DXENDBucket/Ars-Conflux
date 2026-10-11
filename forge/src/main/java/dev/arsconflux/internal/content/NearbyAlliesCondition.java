package dev.arsconflux.internal.content;
import com.google.gson.JsonObject;
import dev.arsconflux.config.ConfluxStartupConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.*;
public final class NearbyAlliesCondition implements ICondition {
    private static final ResourceLocation ID=new ResourceLocation("ars_conflux","nearby_allies_enabled");
    public ResourceLocation getID() {return ID;}
    public boolean test(IContext context) {return ConfluxStartupConfig.nearbyAllies();}
    public static final class Serializer implements IConditionSerializer<NearbyAlliesCondition> {
        public void write(JsonObject json,NearbyAlliesCondition condition) {}
        public NearbyAlliesCondition read(JsonObject json) {return new NearbyAlliesCondition();}
        public ResourceLocation getID() {return ID;}
    }
}
