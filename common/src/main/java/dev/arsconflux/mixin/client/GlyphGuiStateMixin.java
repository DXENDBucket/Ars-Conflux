package dev.arsconflux.mixin.client;
import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import com.hollingsworth.arsnouveau.setup.config.Config;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.arsconflux.client.GlyphGuiState;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=RenderUtils.class,remap=false)
public abstract class GlyphGuiStateMixin {
    @WrapMethod(method="renderItemModel")
    private static void conflux$opacity(PoseStack pose,ItemStack stack,int x,int y,int scale,int alpha,BakedModel model,
            ItemRenderer renderer,boolean transparent,int z,Operation<Void> original) {
        GlyphGuiState.run(transparent && Config.GUI_TRANSPARENCY.get(),()->original.call(pose,stack,x,y,scale,alpha,model,renderer,transparent,z));
    }
}
