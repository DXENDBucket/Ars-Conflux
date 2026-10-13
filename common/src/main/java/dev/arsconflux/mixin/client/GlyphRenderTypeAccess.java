package dev.arsconflux.mixin.client;
import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="net.minecraft.client.renderer.RenderType$CompositeRenderType") public interface GlyphRenderTypeAccess { @Accessor("state") RenderType.CompositeState confluxGlyph$state(); }
