package dev.arsconflux.client;
/** Scoped Ars GUI opacity; survives nested rendering and restores after exceptions. */
public final class GlyphGuiState {
    private static final ThreadLocal<Float> ALPHA=ThreadLocal.withInitial(()->1F);
    public static float alpha(){return ALPHA.get();}
    public static void run(boolean translucent,Runnable draw){
        float previous=ALPHA.get();ALPHA.set(translucent?.25F:1F);try{draw.run();}finally{ALPHA.set(previous);}
    }
    private GlyphGuiState(){}
}
