package dev.arsconflux.internal.neg;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.alexthw.sauce.api.IPropagator;
import com.hollingsworth.arsnouveau.api.spell.*;
import dev.arsconflux.glyph.PropagateMethod;
import dev.arsconflux.config.ConfluxStartupConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** The nested implementation is linked only after the optional NEG mod is confirmed present. */
public final class NegFormGlyphs {
    public static void register(){
        if(!ConfluxStartupConfig.negFormPropagators()||!net.neoforged.fml.ModList.get().isLoaded("not_enough_glyphs"))return;
        if(ConfluxStartupConfig.nearbyAllies())GlyphRegistry.registerSpell(new Form("glyph_propagate_nearby_allies","Propagate Nearby Allies","ars_conflux:glyph_nearby_allies"));
        if(ConfluxStartupConfig.elementalPhalanx()&&hasPhalanx())GlyphRegistry.registerSpell(new Form("glyph_propagate_carian_phalanx","Propagate Carian Phalanx","ars_elemental:glyph_carian_phalanx"));
    }
    private static boolean hasPhalanx(){return NegFormGlyphs.class.getClassLoader().getResource("alexthw/ars_elemental/common/glyphs/MethodCarianPhalanx.class")!=null;}
    private static final class Form extends PropagateMethod implements com.alexthw.sauce.api.IPropagator {
        Form(String id,String name,String method){super(id,name,method);}
        public AbstractCastMethod getCastType(){return method();}
        protected void copy(HitResult hit,Level world,LivingEntity caster,SpellStats stats,SpellContext context,SpellResolver resolver){IPropagator.super.copyResolver(hit,world,caster,stats,context,resolver);}
    }
    private NegFormGlyphs(){}
}
