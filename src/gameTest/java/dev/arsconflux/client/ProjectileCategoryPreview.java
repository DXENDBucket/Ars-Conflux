package dev.arsconflux.client;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.client.gui.book.GlyphFormatter;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.effect.*;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import dev.arsconflux.api.projectile.CarrierRegistry;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import java.util.*;

/** No world, no player save: actual Ars formatter, wheel builder and addon item rendering. */
public final class ProjectileCategoryPreview extends Screen {
    private static boolean opened;private int frames;
    private final List<AbstractSpellPart> samples=new ArrayList<>();
    private final List<AbstractSpellPart> wheel=new ArrayList<>();
    static void openIfReady(){
        var mc=Minecraft.getInstance();
        if(!opened&&Boolean.getBoolean("arsconflux.projectile_category_preview")&&mc.screen!=null&&mc.getOverlay()==null){
            opened=true;mc.setScreen(new ProjectileCategoryPreview());
        }
    }
    private ProjectileCategoryPreview(){super(Component.literal("Projectile glyph category"));GlyphPreviewDefaults.load();}
    private static AbstractSpellPart glyph(String id){
        var part=GlyphRegistry.getSpellPart(CarrierRegistry.id(id));if(part==null)throw new AssertionError(id);return part;
    }
    @Override protected void init(){
        var fireball=glyph("irons_nouveau:glyph_fireball");var lightning=glyph("irons_nouveau:glyph_ball_lightning");
        samples.clear();samples.addAll(List.of(MethodSelf.INSTANCE,MethodProjectile.INSTANCE,fireball,lightning,
                glyph("irons_nouveau:glyph_magic_missile"),AugmentAmplify.INSTANCE,AugmentAOE.INSTANCE,EffectHarm.INSTANCE,EffectHeal.INSTANCE));
        var formatter=new GlyphFormatter(0,40,ignored->{},new ArrayList<>(samples),
                widgets->widgets.forEach(this::removeWidget),this::addRenderableWidget);
        formatter.layoutAllGlyphs(0);
        var matching=GlyphFormatter.CATEGORIES.stream().filter(c->c.filter().test(fireball)).toList();
        if(matching.size()!=1||!matching.get(0).id().toString().equals("ars_conflux:projectile"))throw new AssertionError("Payload heading duplicated or absent");
        var stack=BuiltInRegistries.ITEM.get(CarrierRegistry.id("ars_nouveau:novice_spell_book")).getDefaultInstance();
        var book=(SpellBook)stack.getItem();var caster=book.getSpellCaster(stack);
        caster=caster.setSpell(new Spell(MethodProjectile.INSTANCE,fireball),0);
        caster=caster.setSpell(new Spell(MethodSelf.INSTANCE,EffectOrbit.INSTANCE,lightning),1);
        caster=caster.setSpell(new Spell(MethodProjectile.INSTANCE,EffectHarm.INSTANCE),2);
        caster=caster.setSpell(new Spell(MethodSelf.INSTANCE,EffectHeal.INSTANCE,EffectOrbit.INSTANCE,fireball),3);
        caster.saveToStack(stack);
        var slots=book.getRadialMenuSlotsForSpellpart(stack);
        if(slots.get(0).primarySlotIcon()!=fireball||slots.get(1).primarySlotIcon()!=lightning
                ||slots.get(2).primarySlotIcon()!=EffectHarm.INSTANCE||slots.get(3).primarySlotIcon()!=EffectHeal.INSTANCE)
            throw new AssertionError("Actual native radial slot selection failed: "+slots.subList(0,4).stream().map(s->s.primarySlotIcon()==null?"null":s.primarySlotIcon().getRegistryName().toString()).toList());
        wheel.clear();for(int i=0;i<4;i++)wheel.add(slots.get(i).primarySlotIcon());
        // Exercise the optional Zero category resolver through its own loaded class.
        if(net.neoforged.fml.ModList.get().isLoaded("ars_zero"))
            dev.arsconflux.internal.zero.client.ZeroGlyphCatalog.pages(samples);
        org.slf4j.LoggerFactory.getLogger("Conflux category preview").info("PASS: exclusive category, native wheel payload/orbit/plain/nested selection and live GUI glyph rendering");
    }
    @Override public void render(GuiGraphics gui,int x,int y,float delta){
        gui.fill(0,0,width,height,0xFFE7DECC);
        gui.drawString(font,"Projectile replacement category",10,10,0xFF282828);
        super.render(gui,x,y,delta);
        gui.drawString(font,"Actual Ars category grid",16,34,0xFF282828);
        int right=140;
        gui.drawString(font,"Native wheel primary icons",right,42,0xFF282828);
        String[] labels={"Projectile / Fireball","Orbit / Lightning","Projectile / Harm","Heal / later Fireball"};
        for(int i=0;i<wheel.size();i++){
            gui.renderItem(wheel.get(i).getGlyph().getDefaultInstance(),right,60+i*28);
            gui.drawString(font,labels[i],right+22,64+i*28,0xFF282828);
        }
        gui.drawString(font,"Inventory icons",16,200,0xFF282828);
        for(int i=0;i<samples.size();i++)gui.renderItem(samples.get(i).getGlyph().getDefaultInstance(),16+i*28,220);
        gui.flush();
        if(++frames==30)Screenshot.grab(minecraft.gameDirectory,"projectile-category.png",minecraft.getMainRenderTarget(),ignored->{});
        if(frames==90)minecraft.stop();
    }
}
