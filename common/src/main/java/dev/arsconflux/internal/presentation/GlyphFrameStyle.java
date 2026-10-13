package dev.arsconflux.internal.presentation;
/** Pure presentation values, shared by both loaders and safe to test without Minecraft. */
public record GlyphFrameStyle(boolean enabled,boolean providerIcons,double centerScale,Mode mode,int uniformColor,
                              int shapeColor,int augmentColor,int effectColor) {
    public enum Mode { ARS_TYPE, UNIFORM, CUSTOM }
    public enum Category { SHAPE, AUGMENT, EFFECT }
    public static int nativeColor(Category category){return switch(category){case SHAPE->0xB045E5;case AUGMENT->0xDC9613;case EFFECT->0x1C919A;};}
    public int color(Category category){return mode==Mode.ARS_TYPE?nativeColor(category):mode==Mode.UNIFORM?uniformColor
            :switch(category){case SHAPE->shapeColor;case AUGMENT->augmentColor;case EFFECT->effectColor;};}
    public static boolean validColor(Object value){return value instanceof String && ((String)value).matches("#?[0-9a-fA-F]{6}");}
    public static int parseColor(String value){if(!validColor(value))throw new IllegalArgumentException("Expected RRGGBB or #RRGGBB");return Integer.parseInt(value.replace("#",""),16);}
}
