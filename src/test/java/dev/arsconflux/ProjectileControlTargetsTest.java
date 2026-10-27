package dev.arsconflux;

import java.nio.file.*;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import static org.junit.jupiter.api.Assertions.*;

class ProjectileControlTargetsTest {
    private void check(Path jar,String tick,String define,String descriptor,String gravity) throws Exception {
        try(var zip=new ZipFile(jar.toFile())) {
            var node=new ClassNode();
            try(var input=zip.getInputStream(zip.getEntry("com/hollingsworth/arsnouveau/common/entity/EntityProjectileSpell.class"))) {
                new ClassReader(input.readAllBytes()).accept(node,0);
            }
            assertTrue(node.methods.stream().anyMatch(m->m.name.equals(tick) && m.desc.equals("()V")));
            assertTrue(node.methods.stream().anyMatch(m->m.name.equals(define) && m.desc.equals(descriptor)));
            assertTrue(node.methods.stream().anyMatch(m->m.name.equals(gravity) && m.desc.equals("()Z")));
            assertTrue(node.methods.stream().anyMatch(m->m.name.equals("getExpirationTime") && m.desc.equals("()I")));
            assertTrue(node.fields.stream().anyMatch(f->f.name.equals("age") && f.desc.equals("I")));
            assertTrue(node.fields.stream().anyMatch(f->f.name.equals("isNoGravity") && f.desc.equals("Z")));
        }
    }
    @Test void neoHooksMatchTheNativeArsCarrier() throws Exception {
        check(Path.of("libs/ars.jar"),"tick","defineSynchedData","(Lnet/minecraft/network/syncher/SynchedEntityData$Builder;)V","isNoGravity");
        assertTrue(Files.readString(Path.of("src/main/resources/ars_conflux.mixins.json")).contains("\"ProjectileControlTickMixin\""));
        assertTrue(Files.readString(Path.of("src/main/resources/ars_conflux.mixins.json")).contains("\"ProjectileControlDataMixin\""));
    }
    @Test void forgeHooksMatchTheReleasedSrgCarrier() throws Exception {
        check(Path.of("forge/libs/ars-nouveau-forge1201.jar"),"m_8119_","m_8097_","()V","m_20068_");
        assertTrue(Files.readString(Path.of("forge/src/main/resources/ars_conflux.mixins.json")).contains("\"ProjectileControlTickMixin\""));
        assertTrue(Files.readString(Path.of("forge/src/main/resources/ars_conflux.mixins.json")).contains("\"ProjectileControlDataMixin\""));
    }
}
