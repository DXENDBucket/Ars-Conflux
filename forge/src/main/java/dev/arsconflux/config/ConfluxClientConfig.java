package dev.arsconflux.config;
import net.minecraftforge.common.ForgeConfigSpec;
import dev.arsconflux.internal.presentation.GlyphFrameStyle;
public final class ConfluxClientConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLED,PROVIDER_ICONS;
    private static final ForgeConfigSpec.DoubleValue SCALE;
    private static final ForgeConfigSpec.EnumValue<GlyphFrameStyle.Mode> MODE;
    private static final ForgeConfigSpec.ConfigValue<String> UNIFORM,SHAPE,AUGMENT,EFFECT;
    static {
        var b=new ForgeConfigSpec.Builder();b.push("glyph_icons");
        ENABLED=b.comment("Use Ars' original glyph base and type border for registered Conflux addon GUI icons.").define("frame_enabled",true);
        PROVIDER_ICONS=b.comment("Use addon-provided icon sources, e.g. native spell icons. False uses the original glyph item model.").define("provider_icons",true);
        SCALE=b.comment("Center scale inside the frame. 0.75 renders a 16px icon in 12px. No frame uses full size.").defineInRange("center_scale",.75,.1,1);
        MODE=b.comment("ARS_TYPE uses original Ars border pixels; UNIFORM and CUSTOM recolor only the colored border pixels.").defineEnum("color_mode",GlyphFrameStyle.Mode.ARS_TYPE);
        UNIFORM=b.define("uniform_color","#B79F69",GlyphFrameStyle::validColor);
        SHAPE=b.define("shape_color","#B045E5",GlyphFrameStyle::validColor);
        AUGMENT=b.define("augment_color","#DC9613",GlyphFrameStyle::validColor);
        EFFECT=b.define("effect_color","#1C919A",GlyphFrameStyle::validColor);
        b.pop();SPEC=b.build();
    }
    public static GlyphFrameStyle style() {
        if(!SPEC.isLoaded())return new GlyphFrameStyle(true,true,.75,GlyphFrameStyle.Mode.ARS_TYPE,0xB79F69,0xB045E5,0xDC9613,0x1C919A);
        return new GlyphFrameStyle(ENABLED.get(),PROVIDER_ICONS.get(),SCALE.get(),MODE.get(),
            GlyphFrameStyle.parseColor(UNIFORM.get()),GlyphFrameStyle.parseColor(SHAPE.get()),GlyphFrameStyle.parseColor(AUGMENT.get()),GlyphFrameStyle.parseColor(EFFECT.get()));
    }
    private ConfluxClientConfig(){}
}
