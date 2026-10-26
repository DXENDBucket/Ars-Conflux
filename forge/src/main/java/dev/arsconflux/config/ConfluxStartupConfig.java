package dev.arsconflux.config;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
/** Forge lacks STARTUP configs: read just this file before registration; COMMON writes/validates it later. */
public final class ConfluxStartupConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue AUGMENT_EXTENSIONS;
    private static final boolean EARLY_ALLIES=readEarly();
    static {
        var b=new ForgeConfigSpec.Builder();b.push("optional_content");
        b.comment("Register Nearby Allies and its Scribe recipe. Requires restart; match client/server.").define("enable_nearby_allies",false);
        b.pop();b.push("compatibility");
        AUGMENT_EXTENSIONS=b.comment("Enable optional AAP/NEG augment extensions and weighted Controle delays. Requires restart; match client and server.").define("enable_augment_extensions",true);
        b.pop();SPEC=b.build();
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
    private ConfluxStartupConfig() {}
}
