package dev.arsconflux.api.school;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Immutable, normalized shares. Empty means undeclared, not a hidden default school. */
public final class SchoolWeights {
    public static final SchoolWeights EMPTY = new SchoolWeights(Map.of());
    private final Map<ResourceLocation, Double> shares;
    private SchoolWeights(Map<ResourceLocation, Double> shares) { this.shares = shares; }
    public static SchoolWeights of(ResourceLocation school) { return of(Map.of(school, 1.0)); }
    public static SchoolWeights of(Map<ResourceLocation, ? extends Number> values) {
        Objects.requireNonNull(values, "weights");
        var positive = new TreeMap<ResourceLocation, Double>();
        double maximum = 0;
        for (var entry : values.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "school");
            double n = Objects.requireNonNull(entry.getValue(), "weight").doubleValue();
            if (!Double.isFinite(n) || n < 0) throw new IllegalArgumentException("School weights must be finite and nonnegative");
            if (n > 0) { positive.put(entry.getKey(), n); maximum = Math.max(maximum, n); }
        }
        if (positive.isEmpty()) return EMPTY;
        // Scale first so even multiple Double.MAX_VALUE inputs can be normalized safely.
        double sum = 0;
        for (double n : positive.values()) sum += n / maximum;
        var normalized = new LinkedHashMap<ResourceLocation, Double>();
        for (var entry : positive.entrySet()) normalized.put(entry.getKey(), (entry.getValue() / maximum) / sum);
        return new SchoolWeights(Collections.unmodifiableMap(normalized));
    }
    public static SchoolWeights fromJson(JsonObject json) {
        var values = new LinkedHashMap<ResourceLocation, Double>();
        for (var entry : json.entrySet()) {
            var id = ResourceLocation.tryParse(entry.getKey());
            if (id == null || !entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException("Invalid school weight: " + entry.getKey());
            values.put(id, entry.getValue().getAsDouble());
        }
        return of(values);
    }
    public JsonObject toJson() {
        var json = new JsonObject(); shares.forEach((id, n) -> json.addProperty(id.toString(), n)); return json;
    }
    public Map<ResourceLocation, Double> shares() { return shares; }
    public boolean isEmpty() { return shares.isEmpty(); }
    /** A weighted average of per-school multipliers; undeclared profiles always return 1. */
    public double multiplier(ToDoubleFunction<ResourceLocation> values) {
        if (isEmpty()) return 1;
        double result = 0;
        for (var entry : shares.entrySet()) {
            double value = values.applyAsDouble(entry.getKey());
            if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("School multipliers must be finite and nonnegative");
            result += entry.getValue() * value;
        }
        return result;
    }
    @Override public boolean equals(Object other) { return other instanceof SchoolWeights weights && shares.equals(weights.shares); }
    @Override public int hashCode() { return shares.hashCode(); }
    @Override public String toString() { return shares.toString(); }
}
