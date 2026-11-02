package dev.arsconflux.internal.content;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;
public record RegisteredGlyphCondition(ResourceLocation glyph) implements ICondition {
    public static final MapCodec<RegisteredGlyphCondition> CODEC=ResourceLocation.CODEC.fieldOf("glyph").xmap(RegisteredGlyphCondition::new,RegisteredGlyphCondition::glyph);
    public boolean test(IContext context){return com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.getSpellPart(glyph)!=null;}
    public MapCodec<? extends ICondition> codec(){return CODEC;}
}
