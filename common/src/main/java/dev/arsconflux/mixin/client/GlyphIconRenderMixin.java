package dev.arsconflux.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.arsconflux.client.GlyphGuiRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemRenderer.class)
public abstract class GlyphIconRenderMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void conflux$glyph(ItemStack stack,ItemDisplayContext context,boolean left,PoseStack pose,MultiBufferSource buffers,
            int light,int overlay,BakedModel model,CallbackInfo ci) {
        if(GlyphGuiRenderer.render((ItemRenderer)(Object)this,stack,context,left,pose,buffers,light,overlay,model))ci.cancel();
    }
}
