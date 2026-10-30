package dev.arsconflux.internal.ability;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.Consumer;

/** Atomic server-only hint snapshot. No entity, spell or item registry mutation. */
public final class AbilityData {
    private static volatile Map<ResourceLocation,Set<ResourceLocation>> current=Map.of();
    public static Map<ResourceLocation,Set<ResourceLocation>> current() {return current;}
    public static void clear() {current=Map.of();}
    public static void reload(Map<ResourceLocation,JsonElement> resources,Consumer<String> warning) {
        var next=new HashMap<ResourceLocation,Set<ResourceLocation>>();
        resources.forEach((glyph,json)->{
            try {
                var array=json.getAsJsonObject().getAsJsonArray("tags");
                if(array==null)throw new IllegalArgumentException("Missing tags array");
                var tags=new HashSet<ResourceLocation>();
                for(var entry:array) {
                    if(!entry.isJsonPrimitive()||!entry.getAsJsonPrimitive().isString())
                        throw new IllegalArgumentException("Ability tag must be a resource ID string");
                    var id=ResourceLocation.tryParse(entry.getAsString());
                    if(id==null)throw new IllegalArgumentException("Invalid ability tag: "+entry);
                    tags.add(id);
                }
                next.put(glyph,Set.copyOf(tags));
            } catch(RuntimeException invalid) {warning.accept("Invalid glyph abilities "+glyph+": "+invalid.getMessage());}
        });
        current=Map.copyOf(next);
    }
    private AbilityData() {}
}
