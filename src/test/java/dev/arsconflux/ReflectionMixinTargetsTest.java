package dev.arsconflux;

import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class ReflectionMixinTargetsTest {
    private static ClassNode read(Path jar,String name) throws Exception {
        try(var zip=new ZipFile(jar.toFile())) {
            var node=new ClassNode(); var entry=zip.getEntry(name+".class"); assertNotNull(entry,name);
            try(var input=zip.getInputStream(entry)) { new ClassReader(input.readAllBytes()).accept(node,0); }
            return node;
        }
    }
    @Test void arsCarrierAndHomingHooksMatchTheInstalledRelease() throws Exception {
        var carrier=read(Path.of("libs/ars.jar"),"com/hollingsworth/arsnouveau/common/entity/EntityProjectileSpell");
        assertTrue(carrier.methods.stream().anyMatch(m->m.name.equals("onHit") && m.desc.equals("(Lnet/minecraft/world/phys/HitResult;)V")));
        assertTrue(carrier.methods.stream().anyMatch(m->m.name.equals("attemptRemoval") && m.desc.equals("()V")));
        assertTrue(carrier.fields.stream().anyMatch(f->f.name.equals("redirected") && f.desc.equals("Z")));
        var homing=read(Path.of("libs/ars.jar"),"com/hollingsworth/arsnouveau/common/entity/EntityHomingProjectileSpell");
        assertTrue(homing.fields.stream().anyMatch(f->f.name.equals("target") && f.desc.equals("Lnet/minecraft/world/entity/LivingEntity;")));
        assertTrue(homing.methods.stream().anyMatch(m->m.name.equals("setIgnored") && m.desc.equals("(Ljava/util/List;)V")));
    }
    @Test void damageCreditHookUsesTheNativeGetterAndBothLoadersRegisterTheCommonHooks() throws Exception {
        var source=read(Path.of("build/moddev/artifacts/neoforge-21.1.250-merged.jar"),"net/minecraft/world/damagesource/DamageSource");
        assertTrue(source.methods.stream().anyMatch(m->m.name.equals("getEntity") && m.desc.equals("()Lnet/minecraft/world/entity/Entity;")));
        for(var config:List.of(Path.of("src/main/resources/ars_conflux.mixins.json"),Path.of("forge/src/main/resources/ars_conflux.mixins.json"))) {
            var json=Files.readString(config);
            for(var mixin:List.of("ReflectedDamageCreditMixin","ReflectedCarrierHitMixin","HomingReflectionMixin"))assertTrue(json.contains('"'+mixin+'"'));
        }
    }
    @Test void forgeReleasedCarrierAndHomingHooksRemainCompatibleWithoutZero() throws Exception {
        var jar=Path.of("forge/libs/ars-nouveau-forge1201.jar");
        var carrier=read(jar,"com/hollingsworth/arsnouveau/common/entity/EntityProjectileSpell");
        assertTrue(carrier.methods.stream().anyMatch(m->m.name.equals("m_6532_") && m.desc.equals("(Lnet/minecraft/world/phys/HitResult;)V")));
        assertTrue(carrier.methods.stream().anyMatch(m->m.name.equals("attemptRemoval") && m.desc.equals("()V")));
        var homing=read(jar,"com/hollingsworth/arsnouveau/common/entity/EntityHomingProjectileSpell");
        assertTrue(homing.fields.stream().anyMatch(f->f.name.equals("target") && f.desc.equals("Lnet/minecraft/world/entity/LivingEntity;")));
        assertTrue(homing.methods.stream().anyMatch(m->m.name.equals("getIgnored") && m.desc.equals("()Ljava/util/List;")));
        assertTrue(homing.methods.stream().anyMatch(m->m.name.equals("setIgnored") && m.desc.equals("(Ljava/util/List;)V")));
        assertFalse(Files.readString(Path.of("forge/src/main/resources/ars_conflux.mixins.json")).contains("OptionalZero"));
    }
}
