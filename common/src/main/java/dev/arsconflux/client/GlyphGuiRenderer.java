package dev.arsconflux.client;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.arsconflux.api.glyph.*;
import dev.arsconflux.config.ConfluxClientConfig;
import dev.arsconflux.internal.presentation.GlyphFrameStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.*;
/** One GUI entry for inventory, Ars spell buttons and other callers using the item renderer. */
public final class GlyphGuiRenderer {
    private static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> false);

    public static boolean render(ItemRenderer renderer, ItemStack stack, ItemDisplayContext context, boolean left,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay, BakedModel model) {
        if (context != ItemDisplayContext.GUI || BYPASS.get() || !(stack.getItem() instanceof Glyph glyph)) return false;
        var part = glyph.spellPart;
        if (part == null || !GlyphVisuals.applies(part)) return false;
        var style = ConfluxClientConfig.style();
        var icon = style.providerIcons() ? GlyphVisuals.icon(part) : null;
        if (icon != null && icon.texture() != null
                && Minecraft.getInstance().getResourceManager().getResource(icon.texture()).isEmpty()) icon = null;
        if (!style.enabled() && icon == null) return false;
        var category = part instanceof AbstractCastMethod ? GlyphFrameStyle.Category.SHAPE
                : part instanceof AbstractAugment ? GlyphFrameStyle.Category.AUGMENT : GlyphFrameStyle.Category.EFFECT;
        var frame = style.enabled() ? GlyphFrameTextures.frame(style, category) : null;
        if (frame == null && icon == null) return false;
        float alpha = GlyphGuiState.alpha();
        if (frame != null) GlyphIconPlatform.draw(pose, frame, alpha);
        pose.pushPose();
        boolean previous = BYPASS.get();
        BYPASS.set(true);
        try {
            double scale = frame == null ? 1 : style.centerScale();
            if (icon != null) {
                pose.translate(icon.offsetX(), -icon.offsetY(), 0);
                scale *= icon.scale();
            }
            pose.scale((float) scale, (float) scale, (float) scale);
            if (icon != null && icon.texture() != null) GlyphIconPlatform.draw(pose, icon.texture(), alpha);
            else {
                var source = icon == null ? stack : icon.item();
                var sourceModel = icon == null ? model : renderer.getModel(source, null, Minecraft.getInstance().player, 0);
                // Preserve each native GUI transform once; never call custom-renderer internals with another item's transform.
                renderer.render(source, context, left, pose, alpha < 1 ? GlyphIconPlatform.translucent(alpha) : buffers,
                        light, overlay, sourceModel);
            }
        } finally {
            BYPASS.set(previous);
            pose.popPose();
        }
        return true;
    }

    public static void reload() { GlyphFrameTextures.clear(); }
    private GlyphGuiRenderer() {}
}
