package dev.arsconflux.api.casting;
import net.minecraft.resources.ResourceLocation;
import java.util.Objects;
/** Shared native key, final duration and current remaining ticks; queries never start a cooldown. */
public record CooldownQuote(ResourceLocation key,int duration,long remaining) {
    public CooldownQuote {Objects.requireNonNull(key);if(duration<0||remaining<0)throw new IllegalArgumentException("Negative cooldown");}
}
