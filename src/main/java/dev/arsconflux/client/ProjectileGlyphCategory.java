package dev.arsconflux.client;

import com.hollingsworth.arsnouveau.api.documentation.DocAssets;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.book.GlyphFormatter;
import com.mojang.blaze3d.platform.NativeImage;
import dev.arsconflux.api.glyph.GlyphCategories;
import dev.arsconflux.config.ConfluxClientConfig;
import dev.arsconflux.internal.presentation.GlyphFrameStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Predicate;

/** One exclusive category; icon pixels come from the active Ars resource pack at runtime. */
public final class ProjectileGlyphCategory {
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("ars_conflux","projectile");
    private static final ResourceLocation ICON=ResourceLocation.fromNamespaceAndPath("ars_conflux","dynamic/projectile_category");
    private static int loadedColor=-1;
    private record ExcludingPayload(Predicate<AbstractSpellPart> original) implements Predicate<AbstractSpellPart>{
        public boolean test(AbstractSpellPart part){return !GlyphCategories.projectile(part)&&original.test(part);}
    }
    public static void prepare(){
        var categories=GlyphFormatter.CATEGORIES;
        for(int i=0;i<categories.size();i++){
            var c=categories.get(i);
            if(!c.id().equals(ID)&&!(c.filter() instanceof ExcludingPayload))
                categories.set(i,new GlyphFormatter.Category(c.id(),new ExcludingPayload(c.filter()),c.tooltip(),c.blitInfo()));
        }
        if(categories.stream().noneMatch(c->c.id().equals(ID))){
            var source=DocAssets.AUGMENT_ICON_CRAFTING;
            categories.add(new GlyphFormatter.Category(ID,GlyphCategories::projectile,
                    Component.translatable("ars_conflux.projectile_icon_tooltip"),new DocAssets.BlitInfo(ICON,source.width(),source.height())));
        }
        icon();
    }
    private static void icon(){
        int color=ConfluxClientConfig.style().color(GlyphFrameStyle.Category.PROJECTILE);
        if(color==loadedColor)return;
        var client=Minecraft.getInstance();var source=DocAssets.AUGMENT_ICON_CRAFTING;
        try(var stream=client.getResourceManager().getResourceOrThrow(source.location()).open();var image=NativeImage.read(stream)){
            var result=new NativeImage(source.width(),source.height(),true);
            for(int y=0;y<source.height();y++)for(int x=0;x<source.width();x++){
                int pixel=image.getPixelRGBA(x+source.u(),y+source.v());
                int r=pixel&255,g=pixel>>8&255,b=pixel>>16&255;
                int high=Math.max(r,Math.max(g,b)),low=Math.min(r,Math.min(g,b));
                // Preserve the pale symbol, recoloring only the saturated category background.
                if((high-low)*3>high){
                    double light=high/255.0;
                    r=(int)Math.round((color>>16&255)*light);g=(int)Math.round((color>>8&255)*light);b=(int)Math.round((color&255)*light);
                    pixel=(pixel&0xFF000000)|r|g<<8|b<<16;
                }
                result.setPixelRGBA(x,y,pixel);
            }
            if(loadedColor!=-1)client.getTextureManager().release(ICON);
            client.getTextureManager().register(ICON,new DynamicTexture(result));loadedColor=color;
        }catch(Exception error){org.slf4j.LoggerFactory.getLogger("Ars Conflux").warn("Cannot recolor the projectile category icon",error);}
    }
    public static void reload(){
        boolean used=loadedColor!=-1;
        if(used)Minecraft.getInstance().getTextureManager().release(ICON);
        loadedColor=-1;if(used)icon();
    }
    private ProjectileGlyphCategory(){}
}
