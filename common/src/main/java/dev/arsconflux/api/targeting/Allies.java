package dev.arsconflux.api.targeting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import java.util.*;
import java.util.function.BiPredicate;

/** Symmetric, positive alliance queries. Providers must not recursively call this API.
 * Relation queries do not choose targets, change teams, or override damage rules. */
public final class Allies {
    private static final Map<ResourceLocation,BiPredicate<Entity,Entity>> PROVIDERS=new LinkedHashMap<>();
    private Allies() {}
    /** Register at common setup. Closing removes only this registration, useful for reloadable integrations. */
    public static AutoCloseable register(ResourceLocation id,BiPredicate<Entity,Entity> provider) {
        Objects.requireNonNull(id);Objects.requireNonNull(provider);
        if(PROVIDERS.putIfAbsent(id,provider)!=null) throw new IllegalArgumentException("Duplicate alliance provider: "+id);
        return ()->PROVIDERS.remove(id,provider);
    }
    public static boolean areAllies(Entity first,Entity second) {
        if(first==null || second==null) return false;
        if(first==second || first.isAlliedTo(second) || second.isAlliedTo(first)) return true;
        var a=owners(first);var b=owners(second);
        if(a.stream().anyMatch(b::contains)) return true;
        // Teams of owners also matter for pets belonging to different allied owners.
        if(first instanceof OwnableEntity own && own.getOwner()!=null && own.getOwner()!=first
                && (own.getOwner().isAlliedTo(second) || second.isAlliedTo(own.getOwner()))) return true;
        if(second instanceof OwnableEntity own && own.getOwner()!=null && own.getOwner()!=second
                && (own.getOwner().isAlliedTo(first) || first.isAlliedTo(own.getOwner()))) return true;
        for(var provider:List.copyOf(PROVIDERS.values()))
            if(provider.test(first,second) || provider.test(second,first)) return true;
        return false;
    }
    private static Set<UUID> owners(Entity entity) {
        var ids=new HashSet<UUID>();var visited=new HashSet<UUID>();
        for(int depth=0;entity!=null && depth<8 && visited.add(entity.getUUID());depth++) {
            ids.add(entity.getUUID());
            if(!(entity instanceof OwnableEntity own)) break;
            if(own.getOwnerUUID()!=null) ids.add(own.getOwnerUUID());
            entity=own.getOwner();
        }
        return ids;
    }
}
