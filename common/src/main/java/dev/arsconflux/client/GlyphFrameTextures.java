package dev.arsconflux.client;
import com.mojang.blaze3d.platform.NativeImage;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.internal.presentation.GlyphFrameStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
/** Composes original resource-pack pixels in memory; no Ars art is shipped or written to disk. */
final class GlyphFrameTextures {
    private static final Map<String,ResourceLocation> CACHE=new LinkedHashMap<>();
    private static boolean warned;
    static ResourceLocation frame(GlyphFrameStyle style,GlyphFrameStyle.Category category) {
        String key=category+"/"+style.mode()+"/"+style.color(category);var cached=CACHE.get(key);if(cached!=null)return cached;
        var client=Minecraft.getInstance();String reference=switch(category){case SHAPE->"self";case AUGMENT->"amplify";case EFFECT->"harm";};
        try(var baseStream=client.getResourceManager().getResourceOrThrow(CarrierRegistry.id("ars_nouveau:textures/item/glyph_template.png")).open();
            var borderStream=client.getResourceManager().getResourceOrThrow(CarrierRegistry.id("ars_nouveau:textures/item/"+reference+".png")).open();
            var base=NativeImage.read(baseStream);var border=NativeImage.read(borderStream)) {
            int w=base.getWidth(),h=base.getHeight();var output=new NativeImage(w,h,true);output.copyFrom(base);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                if(x>=Math.max(1,w/8) && x<w-Math.max(1,w/8) && y>=Math.max(1,h/8) && y<h-Math.max(1,h/8))continue;
                int nativePixel=border.getPixelRGBA(x*border.getWidth()/w,y*border.getHeight()/h);
                if(style.mode()!=GlyphFrameStyle.Mode.ARS_TYPE && nativePixel!=base.getPixelRGBA(x,y) && (nativePixel>>>24)!=0) {
                    int nativeColor=GlyphFrameStyle.nativeColor(category),target=style.color(category);
                    double brightness=Math.max(nativePixel&255,Math.max(nativePixel>>8&255,nativePixel>>16&255))
                        /(double)Math.max(nativeColor>>16&255,Math.max(nativeColor>>8&255,nativeColor&255));
                    int red=Math.min(255,(int)Math.round((target>>16&255)*brightness));
                    int green=Math.min(255,(int)Math.round((target>>8&255)*brightness));
                    int blue=Math.min(255,(int)Math.round((target&255)*brightness));
                    nativePixel=(nativePixel&0xFF000000)|red|green<<8|blue<<16;
                }
                output.setPixelRGBA(x,y,nativePixel);
            }
            if(CACHE.size()>=16)clear();
            var location=client.getTextureManager().register("ars_conflux_glyph_frame",new DynamicTexture(output));CACHE.put(key,location);return location;
        }catch(Exception error){if(!warned){org.slf4j.LoggerFactory.getLogger("Ars Conflux").warn("Glyph frame resources unavailable; retaining the original icon",error);warned=true;}return null;}
    }
    static void clear(){var client=Minecraft.getInstance();CACHE.values().forEach(client.getTextureManager()::release);CACHE.clear();warned=false;}
    private GlyphFrameTextures(){}
}
