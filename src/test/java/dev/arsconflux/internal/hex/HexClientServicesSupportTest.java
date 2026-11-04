package dev.arsconflux.internal.hex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.junit.jupiter.api.Assertions.*;

public class HexClientServicesSupportTest {
    public interface TestService {}
    public static class TestProvider implements TestService {
        public TestProvider() {}
    }

    @Test void redirectFindsTheModsProviderEvenWithAnUnrelatedThreadLoader(
            @org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        // Separate service loader models a mod class loader; the thread loader cannot see its provider.
        for (var type : new Class<?>[]{TestService.class, TestProvider.class}) {
            String resource = type.getName().replace('.', '/') + ".class";
            Path file = directory.resolve(resource);
            Files.createDirectories(file.getParent());
            try (var input = type.getClassLoader().getResourceAsStream(resource)) {
                Files.copy(input, file);
            }
        }
        Path descriptor = directory.resolve("META-INF/services/" + TestService.class.getName());
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, TestProvider.class.getName() + "\n");
        var thread = Thread.currentThread();
        var previous = thread.getContextClassLoader();
        try (var modLoader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
             var unrelated = new java.net.URLClassLoader(new java.net.URL[0], ClassLoader.getPlatformClassLoader())) {
            thread.setContextClassLoader(unrelated);
            var service = Class.forName(TestService.class.getName(), true, modLoader);
            assertTrue(java.util.ServiceLoader.load(service).findFirst().isEmpty(), "Original call must reproduce the missing provider");
            var handler = Class.forName("dev.arsconflux.mixin.client.OptionalHexClientServicesMixin")
                    .getDeclaredMethod("arsConflux$clientServices", Class.class);
            handler.setAccessible(true);
            var provider = ((java.util.ServiceLoader<?>)handler.invoke(null, service)).findFirst().orElseThrow();
            assertEquals(TestProvider.class.getName(), provider.getClass().getName());
            assertSame(modLoader, provider.getClass().getClassLoader());
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Test void unknownVersionsAndLegacyPatchNeverReceiveASecondRedirect() {
        for (String version : new String[]{"0.12.0-devel-pre-53", "0.12.0-devel-pre-64", "0.12.0-devel-pre-67"}) {
            assertTrue(HexClientServicesSupport.reviewed(version, false));
            assertFalse(HexClientServicesSupport.reviewed(version, true));
        }
        assertFalse(HexClientServicesSupport.reviewed("", false));
        assertFalse(HexClientServicesSupport.reviewed("0.11.4", false));
        assertFalse(HexClientServicesSupport.reviewed("0.12.0-devel-pre-68", false));
    }

    @Test void releasedHexHasOneOriginalCallAndAnUpstreamFixDisablesTheHook() throws Exception {
        Path jar;
        try (var paths = Files.list(Path.of("libs/hex-link"))) {
            jar = paths.filter(p -> p.getFileName().toString().startsWith("hexcasting-")).findFirst().orElseThrow();
        }
        var node = new ClassNode();
        try (var zip = new ZipFile(jar.toFile()); var input = zip.getInputStream(zip.getEntry(
                "at/petrak/hexcasting/xplat/IClientXplatAbstractions.class"))) {
            new ClassReader(input.readAllBytes()).accept(node, 0);
        }
        assertTrue(HexClientServicesSupport.originalLoader(node));
        var find = node.methods.stream().filter(m -> m.name.equals("find")).findFirst().orElseThrow();
        var call = java.util.Arrays.stream(find.instructions.toArray()).filter(i -> i instanceof MethodInsnNode m
                && m.owner.equals("java/util/ServiceLoader") && m.name.equals("load")).map(i -> (MethodInsnNode)i).findFirst().orElseThrow();
        call.desc = "(Ljava/lang/Class;Ljava/lang/ClassLoader;)Ljava/util/ServiceLoader;";
        assertFalse(HexClientServicesSupport.originalLoader(node));
        call.desc = "(Ljava/lang/Class;)Ljava/util/ServiceLoader;";
        find.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, call.owner, call.name, call.desc, false));
        assertFalse(HexClientServicesSupport.originalLoader(node));
    }
}
