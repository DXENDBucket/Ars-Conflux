package dev.arsconflux;

import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** Check every optional hook against the released jar without initializing Minecraft or Zero. */
class ZeroMixinTargetsTest {
    private static final String MIXIN="Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String WRAP="Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;";
    private static Object property(AnnotationNode annotation,String key) {
        if(annotation.values!=null)for(int i=0;i<annotation.values.size();i+=2)
            if(key.equals(annotation.values.get(i)))return annotation.values.get(i+1);
        return null;
    }
    private static List<AnnotationNode> annotations(MethodNode method) {
        var result=new ArrayList<AnnotationNode>();
        if(method.visibleAnnotations!=null)result.addAll(method.visibleAnnotations);
        if(method.invisibleAnnotations!=null)result.addAll(method.invisibleAnnotations);
        return result;
    }
    private static List<?> list(Object value) { return value instanceof List<?> result?result:List.of(value); }
    private static ClassNode read(byte[] bytes) { var node=new ClassNode(); new ClassReader(bytes).accept(node,0); return node; }
    @Test void released20202ContainsAllOptionalMethodsAndInvocationSites() throws Exception {
        Path jar=Path.of("libs/zero/ars-zero.jar");
        assertTrue(Files.isRegularFile(jar),"Use the official Ars Zero 2.0.2 jar in libs/zero/ars-zero.jar");
        int checkedMethods=0,checkedSites=0,checkedMixins=0;
        try(var zip=new ZipFile(jar.toFile()); var files=Files.walk(Path.of("build/classes/java/main/dev/arsconflux/mixin"))) {
            for(var path:files.filter(p -> p.getFileName().toString().startsWith("OptionalZero") && p.toString().endsWith(".class") && !p.getFileName().toString().contains("$")).toList()) {
                var mixin=read(Files.readAllBytes(path));
                var meta=mixin.invisibleAnnotations.stream().filter(a -> a.desc.equals(MIXIN)).findFirst().orElseThrow();
                var targetName=(String)list(property(meta,"targets")).getFirst();
                var entry=zip.getEntry(targetName.replace('.','/')+".class"); assertNotNull(entry,targetName);
                ClassNode target;
                try(var input=zip.getInputStream(entry)) { target=read(input.readAllBytes()); }
                assertTrue(Files.readString(Path.of("src/main/resources/ars_conflux.mixins.json"))
                        .contains("\""+mixin.name.substring("dev/arsconflux/mixin/".length()).replace('/','.')+"\""));
                checkedMixins++;
                for(var field:mixin.fields) {
                    var annotations=new ArrayList<AnnotationNode>();
                    if(field.visibleAnnotations!=null)annotations.addAll(field.visibleAnnotations);
                    if(field.invisibleAnnotations!=null)annotations.addAll(field.invisibleAnnotations);
                    if(annotations.stream().anyMatch(a->a.desc.endsWith("/Shadow;")))
                        assertTrue(target.fields.stream().anyMatch(f->f.name.equals(field.name) && f.desc.equals(field.desc)),mixin.name+" shadow "+field.name);
                }
                for(var handler:mixin.methods)for(var annotation:annotations(handler)) {
                    if(annotation.desc.endsWith("/Shadow;"))
                        assertTrue(target.methods.stream().anyMatch(m->m.name.equals(handler.name) && m.desc.equals(handler.desc)),mixin.name+" shadow "+handler.name);
                    if(annotation.desc.endsWith("/Accessor;")) {
                        String fieldName=(String)property(annotation,"value");
                        var field=target.fields.stream().filter(f -> f.name.equals(fieldName)).findFirst().orElseThrow();
                        assertEquals(Type.getType(field.desc),Type.getReturnType(handler.desc));
                        assertEquals(field.access&Opcodes.ACC_STATIC,handler.access&Opcodes.ACC_STATIC);
                    }
                    if(annotation.desc.endsWith("/Invoker;")) {
                        String methodName=(String)property(annotation,"value");
                        var method=target.methods.stream().filter(m -> m.name.equals(methodName) && m.desc.equals(handler.desc)).findFirst().orElseThrow();
                        assertEquals(method.access&Opcodes.ACC_STATIC,handler.access&Opcodes.ACC_STATIC);
                    }
                    var selectors=property(annotation,"method");
                    if(selectors==null)continue;
                    for(var selector:list(selectors)) {
                        String spec=(String)selector; int descriptor=spec.indexOf('(');
                        String name=descriptor<0?spec:spec.substring(0,descriptor);
                        var candidates=target.methods.stream().filter(m -> m.name.equals(name)
                                && (descriptor<0 || m.desc.equals(spec.substring(descriptor)))).toList();
                        assertFalse(candidates.isEmpty(),mixin.name+" -> "+spec);
                        checkedMethods++;
                        if(annotation.desc.equals(WRAP)) {
                            assertEquals(1,candidates.size(),"WrapMethod must be unambiguous");
                            var nativeMethod=candidates.getFirst();
                            var args=new ArrayList<>(List.of(Type.getArgumentTypes(handler.desc)));
                            assertEquals("com.llamalad7.mixinextras.injector.wrapoperation.Operation",args.removeLast().getClassName());
                            assertEquals(List.of(Type.getArgumentTypes(nativeMethod.desc)),args,mixin.name+" signature "+spec);
                            assertEquals(Type.getReturnType(nativeMethod.desc),Type.getReturnType(handler.desc));
                            assertEquals(nativeMethod.access&Opcodes.ACC_STATIC,handler.access&Opcodes.ACC_STATIC);
                        }
                        var atValue=property(annotation,"at");
                        if(atValue==null)continue;
                        for(var atObject:list(atValue)) {
                            var at=(AnnotationNode)atObject;
                            String kind=(String)property(at,"value");
                            if(!"INVOKE".equals(kind) && !"FIELD".equals(kind))continue;
                            String invoke=(String)property(at,"target");
                            int ownerEnd=invoke.indexOf(';');
                            String owner=invoke.substring(1,ownerEnd), method=invoke.substring(ownerEnd+1);
                            boolean found=false;
                            for(var nativeMethod:candidates)for(var insn:nativeMethod.instructions)
                                if(insn instanceof MethodInsnNode call && call.owner.equals(owner) && (call.name+call.desc).equals(method)
                                        || insn instanceof FieldInsnNode field && field.owner.equals(owner) && (field.name+":"+field.desc).equals(method))found=true;
                            assertTrue(found,mixin.name+" missing call "+invoke+" in "+spec);
                            checkedSites++;
                        }
                    }
                }
            }
        }
        assertEquals(28,checkedMixins);
        assertTrue(checkedMethods>=30); assertTrue(checkedSites>=9);
    }
}
