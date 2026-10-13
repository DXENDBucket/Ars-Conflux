package dev.arsconflux.mixin.client;
import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(RenderType.CompositeState.class) public interface GlyphCompositeStateAccess { @Accessor("textureState") RenderStateShard.EmptyTextureStateShard confluxGlyph$texture(); }
