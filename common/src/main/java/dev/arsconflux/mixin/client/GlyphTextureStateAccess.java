package dev.arsconflux.mixin.client;
import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(RenderStateShard.TextureStateShard.class) public interface GlyphTextureStateAccess { @Accessor("texture") java.util.Optional<net.minecraft.resources.ResourceLocation> confluxGlyph$location(); }
