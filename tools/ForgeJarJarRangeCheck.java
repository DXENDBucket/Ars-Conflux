import net.minecraftforge.jarjar.selection.JarSelector;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;

/** Uses the real Forge selector against release metadata; no Minecraft server needed. */
public final class ForgeJarJarRangeCheck {
    record Candidate(String id, byte[] metadata) {}
    static Candidate artifact(String path) throws Exception {
        try (var zip=new ZipFile(path)) {
            return new Candidate(path,zip.getInputStream(zip.getEntry("META-INF/jarjar/metadata.json")).readAllBytes());
        }
    }
    static List<Candidate> select(List<Candidate> sources) {
        return JarSelector.detectAndSelect(sources,
                (source,path)->source.metadata()!=null && path.toString().replace('\\','/').equals("META-INF/jarjar/metadata.json")
                        ?Optional.of(new ByteArrayInputStream(source.metadata())):Optional.empty(),
                (source,path)->Optional.of(new Candidate(source.id()+"!"+path,null)),
                Candidate::id,failures->new IllegalStateException(failures.toString()));
    }
    public static void main(String[] args) throws Exception {
        var core=artifact(args[0]);var iron=artifact(args[1]);
        for(var source:List.of(core,iron)) {
            String text=new String(source.metadata(),StandardCharsets.UTF_8);
            if(!text.contains("[0.4.1,)"))throw new AssertionError("Missing unbounded 0.4.1 range: "+source.id());
        }
        var newer=new Candidate("other_mod_0.5.5",("{\"jars\":[{\"identifier\":{\"group\":\"io.github.llamalad7\",\"artifact\":\"mixinextras-forge\"},\"version\":{\"range\":\"[0.5.5,)\",\"artifactVersion\":\"0.5.5\"},\"path\":\"META-INF/jarjar/mixinextras-forge-0.5.5.jar\",\"isObfuscated\":false}]}").getBytes(StandardCharsets.UTF_8));
        var fallback=select(List.of(core,iron));
        if(fallback.size()!=1 || !fallback.get(0).id().contains("0.4.1.jar"))throw new AssertionError("Invalid 0.4.1 fallback: "+fallback);
        var upgraded=select(List.of(core,iron,newer));
        if(upgraded.size()!=1 || !upgraded.get(0).id().contains("0.5.5.jar"))throw new AssertionError("Failed newer selection: "+upgraded);
        var old=new Candidate("old_capped_core",new String(core.metadata(),StandardCharsets.UTF_8).replace("[0.4.1,)","[0.4.1,0.5)").getBytes(StandardCharsets.UTF_8));
        try {select(List.of(old,iron,newer));throw new AssertionError("Old cap should reproduce the report");}
        catch(IllegalStateException expected) {System.out.println("Old capped range reproduces Forge selector failure.");}
        System.out.println("PASS: actual release metadata selects fallback 0.4.1 or shared 0.5.5 without conflict.");
    }
}
