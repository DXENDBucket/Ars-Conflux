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
        v.addVertex(matrix,x,y,0).setColor(255,255,255,Math.round(255*alpha)).setUv(u,t).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,0,1);
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
        public VertexConsumer addVertex(float x,float y,float z){delegate.addVertex(x,y,z);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){delegate.setColor(r,g,b,Math.round(a*alpha));return this;}
        public VertexConsumer setUv(float u,float v){delegate.setUv(u,v);return this;}
        public VertexConsumer setUv1(int u,int v){delegate.setUv1(u,v);return this;}
        public VertexConsumer setUv2(int u,int v){delegate.setUv2(u,v);return this;}
        public VertexConsumer setNormal(float x,float y,float z){delegate.setNormal(x,y,z);return this;}
    }
    private GlyphIconPlatform(){}
}
