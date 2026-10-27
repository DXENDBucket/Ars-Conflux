package dev.arsconflux.internal.ans;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;
import java.util.List;
import java.util.Set;

/** Inspect bytes without loading optional mod classes. Older ANS layouts stay untouched. */
public final class AnsMixinPlugin implements IMixinConfigPlugin {
    private Boolean supported;
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.contains(".OptionalZero")) {
            try {
                return (boolean) Class.forName("dev.arsconflux.internal.zero.ZeroMixinSupport")
                        .getMethod("supported").invoke(null);
            } catch (ReflectiveOperationException unavailable) { return false; }
        }
        if (!mixin.contains(".OptionalAns")) return true;
        if (supported == null) {
            try {
                var node = MixinService.getService().getBytecodeProvider()
                        .getClassNode("com.otectus.arsnspells.spell.CrossCastingHandler");
                supported = node.methods.stream().anyMatch(m -> m.name.equals("castArsSpell") &&
                        (m.desc.contains("Lcom/otectus/arsnspells/spell/target/CastTargetHint;")
                        || m.desc.contains("Lcom/otectus/arsnspells/spell/target/InteractionOrigin;")));
            } catch (ClassNotFoundException | java.io.IOException absent) { supported = false; }
        }
        return supported;
    }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
}
