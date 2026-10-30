package dev.arsconflux.config;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
/** Forge lacks STARTUP configs: read just this file before registration; COMMON writes/validates it later. */
public final class ConfluxStartupConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> SCHOOL_POWER_ATTRIBUTES;
    public static final ForgeConfigSpec.BooleanValue AUGMENT_EXTENSIONS;
    private static final boolean EARLY_ALLIES=readEarly();
    static {
        var b=new ForgeConfigSpec.Builder();b.push("optional_content");
        b.comment("Register Nearby Allies and its Scribe recipe. Requires restart; match client/server.").define("enable_nearby_allies",false);
        b.pop();b.push("compatibility");
        AUGMENT_EXTENSIONS=b.comment("Enable optional AAP/NEG augment extensions and weighted Controle delays. Requires restart; match client and server.").define("enable_augment_extensions",true);
        b.pop();
        b.push("schools");
        SCHOOL_POWER_ATTRIBUTES=b.comment("School IDs to register percentage spell-power attributes for, e.g. example:radiance.",
                "Empty by default. Requires restart and matching client/server lists. Data definitions and glyph weights still support reload.")
                .defineListAllowEmpty("power_attributes", java.util.List.of(), value -> value instanceof String text && net.minecraft.resources.ResourceLocation.tryParse(text)!=null);
        b.pop(); SPEC = b.build();
    }
    private static boolean readEarly() {
        var file=FMLPaths.CONFIGDIR.get().resolve("ars_conflux-startup.toml");
        if(!java.nio.file.Files.isRegularFile(file)) return false;
        try(var config=CommentedFileConfig.builder(file).sync().build()) {
            config.load();return Boolean.TRUE.equals(config.get("optional_content.enable_nearby_allies"));
        }
    }
    public static boolean nearbyAllies() {return EARLY_ALLIES;}
    public static boolean augmentExtensions() {return SPEC.isLoaded()?AUGMENT_EXTENSIONS.get():AUGMENT_EXTENSIONS.getDefault();}
    public static java.util.List<String> schoolPowerAttributes() {
        var file=FMLPaths.CONFIGDIR.get().resolve("ars_conflux-startup.toml");
        if(!java.nio.file.Files.isRegularFile(file)) return java.util.List.of();
        try(var config=CommentedFileConfig.builder(file).sync().build()) {
            config.load();Object values=config.get("schools.power_attributes");
            return values instanceof java.util.List<?> list ? list.stream().filter(String.class::isInstance).map(String.class::cast)
                    .filter(text->net.minecraft.resources.ResourceLocation.tryParse(text)!=null).distinct().sorted().toList() : java.util.List.of();
        }
    }
    private ConfluxStartupConfig() {}
}
