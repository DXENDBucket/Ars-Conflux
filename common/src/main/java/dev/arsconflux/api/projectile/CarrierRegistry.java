package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityOrbitProjectile;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.context.ResolverContexts;
import dev.arsconflux.api.context.ResolverExtension;
import dev.arsconflux.platform.ArsAccess;
import net.minecraft.resources.ResourceLocation;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CarrierRegistry {
    private static final Map<ResourceLocation, CarrierProfile> PROFILES = new LinkedHashMap<>();
    private CarrierRegistry() {}
    /** Register during mod setup, before server execution. Duplicate declarations fail explicitly. */
    public static CarrierProfile register(CarrierProfile profile) {
        if (PROFILES.putIfAbsent(profile.glyphId(), profile) != null)
            throw new IllegalArgumentException("Duplicate carrier " + profile.glyphId());
        return profile;
    }
    public static Collection<CarrierProfile> profiles() { return List.copyOf(PROFILES.values()); }
    public static CarrierProfile get(ResourceLocation id) { return PROFILES.get(id); }
    public static CarrierProfile of(AbstractSpellPart action) { return action == null ? null : get(action.getRegistryName()); }
    private static final ResourceLocation SOURCE = id("ars_conflux:carrier_source");
    private record Source(ResourceLocation glyph) implements ResolverExtension {
        public ResourceLocation id() { return SOURCE; }
    }
    /** Declare a generated carrier's actual form when its synthetic recipe cannot identify it. */
    public static void bind(SpellResolver resolver, ResourceLocation glyph) {
        if (get(glyph) == null) throw new IllegalArgumentException("Unknown carrier " + glyph);
        ResolverContexts.attach(resolver, new Source(glyph));
    }
    public static CarrierProfile of(SpellResolver resolver) {
        if (resolver == null) return null;
        Source source = resolver.spellContext.getAttachment(SOURCE);
        return source == null ? of(resolver.castType) : get(source.glyph());
    }
    public static CarrierProfile of(EntityProjectileSpell carrier) {
        if (carrier instanceof EntityOrbitProjectile) return get(id("ars_nouveau:glyph_orbit"));
        var resolver = ArsAccess.resolver(carrier);
        if (resolver == null) return null;
        Source source = resolver.spellContext.getAttachment(SOURCE);
        if (source != null) return get(source.glyph());
        var profile = of(resolver.castType);
        if (profile == null) return null;
        // Propagators prepend a synthetic augment and set castType to the emitted form.
        if (ArsAccess.startsWithAugment(resolver.spell))
            for (var candidate : PROFILES.values())
                if (profile.glyphId().equals(candidate.emitted())) return candidate;
        return profile;
    }
    public static ResourceLocation id(String value) { return java.util.Objects.requireNonNull(ResourceLocation.tryParse(value)); }
}
