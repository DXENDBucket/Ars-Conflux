package dev.arsconflux.internal.school;

import com.google.gson.*;
import dev.arsconflux.api.school.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.Consumer;

/** One atomic server reload snapshot; deleting data restores code defaults. */
public final class SchoolData {
    public record Snapshot(Map<ResourceLocation, SchoolDefinition> definitions,
                           Map<ResourceLocation, SchoolWeights> glyphs,
                           Map<ResourceLocation, List<SchoolDamageRule>> rules) {}
    private static volatile Snapshot current = new Snapshot(Map.of(), Map.of(), Map.of());
    public static Snapshot current() { return current; }
    public static void clear() { current = new Snapshot(Map.of(), Map.of(), Map.of()); }
    public static void reload(Map<ResourceLocation, JsonElement> resources, Consumer<String> warning) {
        var definitions = new TreeMap<ResourceLocation, SchoolDefinition>();
        var glyphs = new TreeMap<ResourceLocation, SchoolWeights>();
        var rules = new TreeMap<ResourceLocation, List<SchoolDamageRule>>();
        resources.forEach((location, element) -> {
            String path = location.getPath();
            try {
                if (!path.startsWith("schools/") && !path.startsWith("glyph_schools/") && !path.startsWith("school_damage/")) return;
                var id = parseId(location.getNamespace() + ":" + path.substring(path.indexOf('/') + 1));
                var json = element.getAsJsonObject();
                if (path.startsWith("schools/")) {
                    int color = json.has("color") ? color(json.get("color")) : 0xCCCCCC;
                    var nativeIds = new LinkedHashSet<String>();
                    if (json.has("ars_schools")) for (var value : json.getAsJsonArray("ars_schools")) nativeIds.add(value.getAsString());
                    definitions.put(id, new SchoolDefinition(id, string(json,"name",id.toString()), string(json,"translation_key",""),color,nativeIds));
                } else if (path.startsWith("glyph_schools/")) {
                    glyphs.put(id, SchoolWeights.fromJson(json.getAsJsonObject("weights")));
                } else {
                    var school = parseId(json.get("school").getAsString());
                    var rule = new SchoolDamageRule(id, school, optionalId(json,"target_entity"), optionalId(json,"target_tag"),
                            json.has("priority") ? json.get("priority").getAsInt() : 0, json.get("multiplier").getAsDouble());
                    rules.computeIfAbsent(school, ignored -> new ArrayList<>()).add(rule);
                }
            } catch (RuntimeException error) { warning.accept("Invalid Conflux school resource " + location + ": " + error.getMessage()); }
        });
        var sorted = new LinkedHashMap<ResourceLocation, List<SchoolDamageRule>>();
        rules.forEach((id,list) -> sorted.put(id,list.stream().sorted(Comparator.comparingInt(SchoolDamageRule::priority).reversed().thenComparing(r -> r.id().toString())).toList()));
        current = new Snapshot(Map.copyOf(definitions),Map.copyOf(glyphs),Map.copyOf(sorted));
    }
    private static int color(JsonElement value) {
        int color = value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : Integer.parseInt(value.getAsString().replaceFirst("^#", ""),16);
        if (color < 0 || color > 0xFFFFFF) throw new IllegalArgumentException("Invalid RGB color"); return color;
    }
    private static String string(JsonObject json,String key,String fallback) { return json.has(key)?json.get(key).getAsString():fallback; }
    private static ResourceLocation optionalId(JsonObject json,String key) { return json.has(key)?parseId(json.get(key).getAsString()):null; }
    private static ResourceLocation parseId(String text) { var id=ResourceLocation.tryParse(text); if(id==null)throw new IllegalArgumentException("Invalid resource ID: "+text);return id; }
    private SchoolData() {}
}
