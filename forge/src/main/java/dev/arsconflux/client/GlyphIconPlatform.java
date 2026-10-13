package dev.arsconflux.client;
import com.mojang.blaze3d.vertex.*;
import dev.arsconflux.mixin.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
/** Platform vertex plumbing; native GUI transforms and center sources stay in the shared driver. */
final class GlyphIconPlatform {
    static void draw(PoseStack pose,ResourceLocation texture,float alpha) {
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var type=RenderType.entityTranslucentEmissive(texture,false);
        var v=buffers.getBuffer(type);var m=pose.last().pose();
        vertex(v,m,-.5F,-.5F,0,1,alpha);vertex(v,m,.5F,-.5F,1,1,alpha);
        vertex(v,m,.5F,.5F,1,0,alpha);vertex(v,m,-.5F,.5F,0,0,alpha);
        // Paint the base first without depth writes, so it cannot hide the rear half of a 3D center model.
        buffers.endBatch(type);
    }
    private static void vertex(VertexConsumer v,Matrix4f matrix,float x,float y,float u,float t,float alpha){
        v.vertex(matrix,x,y,0).color(255,255,255,Math.round(255*alpha)).uv(u,t).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,0,1).endVertex();
    }
    static MultiBufferSource translucent(float alpha) {
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        return original->{
            RenderType target=original;
            if(original instanceof GlyphRenderTypeAccess) {
                var state=((GlyphRenderTypeAccess)original).confluxGlyph$state();
                var textureState=((GlyphCompositeStateAccess)(Object)state).confluxGlyph$texture();
                if(textureState instanceof GlyphTextureStateAccess access) {
                    var texture=access.confluxGlyph$location();
                    if(texture.isPresent())target=RenderType.entityTranslucentEmissive(texture.get(),false);
                }
            }
            return new Alpha(buffers.getBuffer(target),alpha);
        };
    }
    private record Alpha(VertexConsumer delegate,float alpha) implements VertexConsumer {
        public VertexConsumer vertex(double x,double y,double z){delegate.vertex(x,y,z);return this;}
        public VertexConsumer color(int r,int g,int b,int a){delegate.color(r,g,b,Math.round(a*alpha));return this;}
        public VertexConsumer uv(float u,float v){delegate.uv(u,v);return this;}
        public VertexConsumer overlayCoords(int u,int v){delegate.overlayCoords(u,v);return this;}
        public VertexConsumer uv2(int u,int v){delegate.uv2(u,v);return this;}
        public VertexConsumer normal(float x,float y,float z){delegate.normal(x,y,z);return this;}
        public void endVertex(){delegate.endVertex();}
        public void defaultColor(int r,int g,int b,int a){delegate.defaultColor(r,g,b,Math.round(a*alpha));}
        public void unsetDefaultColor(){delegate.unsetDefaultColor();}
    }
    private GlyphIconPlatform(){}
}
