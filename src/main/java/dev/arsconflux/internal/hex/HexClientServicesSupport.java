package dev.arsconflux.internal.hex;

import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.service.MixinService;

/** Inspect discovery metadata and bytecode without initializing Hex's client platform service. */
public final class HexClientServicesSupport {
    private static final Set<String> REVIEWED_VERSIONS = Set.of(
            "0.12.0-devel-pre-53", "0.12.0-devel-pre-64", "0.12.0-devel-pre-67");

    private HexClientServicesSupport() {}

    public static boolean supported(String target) {
        var mods = LoadingModList.get();
        if (mods == null) return false;
        boolean legacy = mods.getMods().stream().anyMatch(m -> m.getModId().equals("hex_pre53_client_fix"));
        String version = mods.getMods().stream().filter(m -> m.getModId().equals("hexcasting"))
                .map(m -> m.getVersion().toString()).findFirst().orElse("");
        if (!reviewed(version, legacy)) return false;
        try {
            return originalLoader(MixinService.getService().getBytecodeProvider().getClassNode(target));
        } catch (ClassNotFoundException | java.io.IOException absent) {
            return false;
        }
    }

    static boolean reviewed(String version, boolean legacy) {
        return !legacy && REVIEWED_VERSIONS.contains(version);
    }

    static boolean originalLoader(ClassNode target) {
        var methods = target.methods.stream().filter(m -> m.name.equals("find")
                && m.desc.equals("()Lat/petrak/hexcasting/xplat/IClientXplatAbstractions;")
                && (m.access & Opcodes.ACC_STATIC) != 0).toList();
        if (methods.size() != 1) return false;
        int calls = 0;
        for (var instruction : methods.getFirst().instructions) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESTATIC
                    && call.owner.equals("java/util/ServiceLoader") && call.name.equals("load")
                    && call.desc.equals("(Ljava/lang/Class;)Ljava/util/ServiceLoader;")) calls++;
        }
        return calls == 1;
    }
}
