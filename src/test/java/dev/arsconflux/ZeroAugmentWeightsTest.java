package dev.arsconflux;

import dev.arsconflux.internal.zero.ZeroAugmentWeights;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class ZeroAugmentWeightsTest {
    @Test void registeredEquivalentsMatchTheReleasedNativeModifiers() throws Exception {
        var classes=Map.of("augment_amplify_two","AugmentAmplifyTwo","augment_amplify_three","AugmentAmplifyThree",
                "augment_aoe_two","AugmentAOETwo","augment_aoe_three","AugmentAOEThree");
        try(var zip=new ZipFile(Path.of("libs/zero/ars-zero.jar").toFile())) {
            for(var rule:ZeroAugmentWeights.definitions()) {
                var node=new ClassNode();
                try(var input=zip.getInputStream(zip.getEntry("com/github/ars_zero/common/glyph/augment/"+classes.get(rule.glyph())+".class"))) {
                    new ClassReader(input.readAllBytes()).accept(node,0);
                }
                var modifier=node.methods.stream().filter(m->m.name.equals("applyModifiers")).findFirst().orElseThrow();
                var expected=rule.base().equals("glyph_amplify")?"addAmplification":"addAOE";
                boolean matched=false;
                for(var instruction:modifier.instructions)if(instruction instanceof MethodInsnNode call && call.name.equals(expected)) {
                    assertInstanceOf(LdcInsnNode.class,call.getPrevious());
                    assertEquals((double)rule.units(),((Number)((LdcInsnNode)call.getPrevious()).cst).doubleValue(),1e-9);
                    matched=true;
                }
                assertTrue(matched,rule.glyph());
            }
        }
    }
}
