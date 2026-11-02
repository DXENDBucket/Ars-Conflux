package dev.arsconflux.glyph;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import dev.arsconflux.api.context.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import java.util.*;

/** NEG owns child slicing/cancellation; the original form owns target selection and execution. */
public abstract class PropagateMethod extends AbstractEffect {
    private final ResourceLocation methodId;
    protected PropagateMethod(String id,String name,String method){super(ResourceLocation.tryParse("ars_conflux:"+id),name);methodId=ResourceLocation.tryParse(method);}
    public AbstractCastMethod method(){var part=GlyphRegistry.getSpellPart(methodId);return part instanceof AbstractCastMethod m?m:null;}
    public int getDefaultManaCost(){return 100;}
    public SpellTier defaultTier(){return SpellTier.THREE;}
    public Integer getTypeIndex(){return 8;}
    public Set<SpellSchool> getSchools(){return Set.of(SpellSchools.MANIPULATION);}
    public Set<AbstractAugment> getCompatibleAugments(){return getRegistryName().getPath().endsWith("nearby_allies")?Set.of(AugmentAOE.INSTANCE):Set.of(AugmentSplit.INSTANCE);}
    public String getBookDescription(){return net.minecraft.network.chat.Component.translatable("ars_conflux.glyph_desc."+getRegistryName().getPath()).getString();}
    public void addAugmentDescriptions(Map<AbstractAugment,String> out){
        out.put(getRegistryName().getPath().endsWith("nearby_allies")?AugmentAOE.INSTANCE:AugmentSplit.INSTANCE,"Uses the original form's augment behavior.");
    }
    public final void onResolve(HitResult hit,Level world,LivingEntity caster,SpellStats stats,SpellContext context,SpellResolver resolver){
        if(world.isClientSide)return;
        // A disabled/missing destination must not fall through to effects on the old target.
        copy(hit,world,caster,stats,context,resolver);
    }
    protected abstract void copy(HitResult hit,Level world,LivingEntity caster,SpellStats stats,SpellContext context,SpellResolver resolver);
    public final void propagate(Level world,HitResult hit,LivingEntity ignored,SpellStats stats,SpellResolver resolver){
        var target=method();if(world.isClientSide||target==null||!target.isEnabled())return;
        var frame=CastContext.of(resolver.spellContext,hit,null);
        var caster=frame.caster();if(caster==null||!caster.isAlive())return;
        // Like NEG Propagate Self, these caster-bound forms retain their native caster-centered semantics.
        frame=frame.withExecutor(caster);CastContexts.bind(resolver.spellContext,frame);
        resolver.castType=target;
        var scope=frame;
        CastContexts.run(scope,()->target.onCast(ItemStack.EMPTY,caster,world,stats,resolver.spellContext,resolver));
    }
}
