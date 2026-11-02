package dev.arsconflux.internal.content;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.*;
public record RegisteredGlyphCondition(ResourceLocation glyph) implements ICondition {
    private static final ResourceLocation ID=new ResourceLocation("ars_conflux","glyph_registered");
    public ResourceLocation getID(){return ID;}
    public boolean test(IContext context){return com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.getSpellPart(glyph)!=null;}
    public static final class Serializer implements IConditionSerializer<RegisteredGlyphCondition>{
        public void write(JsonObject json,RegisteredGlyphCondition c){json.addProperty("glyph",c.glyph.toString());}
        public RegisteredGlyphCondition read(JsonObject json){return new RegisteredGlyphCondition(new ResourceLocation(json.get("glyph").getAsString()));}
        public ResourceLocation getID(){return ID;}
    }
}
