package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import net.minecraft.resources.ResourceLocation;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Extensible declaration rather than a closed enumeration of addon glyphs. */
public final class CarrierProfile {
    private final ResourceLocation glyphId;
    private final boolean arsDriven;
    private final boolean repeated;
    private final ResourceLocation emitted;
    private final Supplier<Set<AbstractAugment>> augments;
    private Set<AbstractAugment> originalAugments;
    public CarrierProfile(ResourceLocation glyphId, boolean arsDriven, boolean repeated,
                          ResourceLocation emitted, Supplier<Set<AbstractAugment>> augments) {
        this.glyphId = Objects.requireNonNull(glyphId); this.arsDriven = arsDriven;
        this.repeated = repeated; this.emitted = emitted; this.augments = Objects.requireNonNull(augments);
    }
    public ResourceLocation glyphId() { return glyphId; }
    public boolean arsDriven() { return arsDriven; }
    public boolean repeated() { return repeated; }
    public boolean propagator() { return emitted != null; }
    public ResourceLocation emitted() { return emitted; }
    public AbstractSpellPart glyph() { return GlyphRegistry.getSpellPart(glyphId); }
    public Set<AbstractAugment> augments() { return Set.copyOf(augments.get()); }
    /** Capture once, before any participating addon adds its payload augments. */
    public void rememberOriginalAugments(AbstractSpellPart glyph) {
        if (originalAugments == null) originalAugments = Set.copyOf(glyph.compatibleAugments);
    }
    public boolean originallySupported(AbstractAugment augment) { return originalAugments != null && originalAugments.contains(augment); }
}
