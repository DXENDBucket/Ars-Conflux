package dev.arsconflux.api.school;

import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import dev.arsconflux.internal.school.SchoolData;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Open namespaced catalog. Datapacks override display metadata supplied by code. */
public final class SchoolRegistry {
    private static final Map<ResourceLocation, SchoolDefinition> DEFAULTS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Set<SpellSchool>> NATIVE = new LinkedHashMap<>();
    public static synchronized void register(SchoolDefinition definition) {
        var old = DEFAULTS.putIfAbsent(definition.id(), definition);
        if (old != null && !old.equals(definition)) throw new IllegalArgumentException("School already registered: " + definition.id());
    }
    public static synchronized Map<ResourceLocation, SchoolDefinition> all() {
        var result = new TreeMap<>(DEFAULTS); result.putAll(SchoolData.current().definitions());
        return Collections.unmodifiableMap(result);
    }
    public static Optional<SchoolDefinition> get(ResourceLocation id) { return Optional.ofNullable(all().get(id)); }
    /** Explicit, many-to-many association. Does not rewrite native schools or infer any weights. */
    public static synchronized void associate(ResourceLocation id, SpellSchool nativeSchool) {
        Objects.requireNonNull(id); Objects.requireNonNull(nativeSchool);
        NATIVE.computeIfAbsent(id, ignored -> new LinkedHashSet<>()).add(nativeSchool);
    }
    public static synchronized Set<String> nativeSchoolIds(ResourceLocation id) {
        var result = new TreeSet<String>(); get(id).ifPresent(def -> result.addAll(def.arsSchools()));
        NATIVE.getOrDefault(id, Set.of()).forEach(school -> result.add(school.getId()));
        return Collections.unmodifiableSet(result);
    }
    private SchoolRegistry() {}
}
