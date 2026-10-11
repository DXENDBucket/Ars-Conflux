package dev.arsconflux.glyph;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import dev.arsconflux.platform.ArsAccess;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.targeting.Allies;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;

/** Fan out independent continuations; selected allies never become the owner or payer. */
public final class MethodNearbyAllies extends AbstractCastMethod {
    public static final MethodNearbyAllies INSTANCE=new MethodNearbyAllies();
    private MethodNearbyAllies() {super(ResourceLocation.tryParse("ars_conflux:glyph_nearby_allies"),"Nearby Allies");}
    public int getDefaultManaCost() {return 30;}
    public SpellTier defaultTier() {return SpellTier.THREE;}
    public Set<AbstractAugment> getCompatibleAugments() {return augmentSetOf(AugmentAOE.INSTANCE);}
    public String getBookDescription() {return net.minecraft.network.chat.Component.translatable("ars_conflux.glyph_desc.glyph_nearby_allies").getString();}
    public static double radius(SpellStats stats) {return Math.max(1,Math.min(32,8+3*stats.getAoeMultiplier()));}
    public CastResolveType onCast(ItemStack stack,LivingEntity executor,Level world,SpellStats stats,SpellContext context,SpellResolver resolver) {
        if(world.isClientSide) return CastResolveType.SUCCESS;
        var frame=CastContext.of(context,null,null).withExecutor(executor);
        if(frame.caster()==null || executor==null) return CastResolveType.SUCCESS;
        double radius=radius(stats);var center=executor.position();
        var targets=world.getEntitiesOfClass(LivingEntity.class,executor.getBoundingBox().inflate(radius),
                target->target!=executor && target!=frame.caster() && target.isAlive() && !target.isSpectator()
                        && target.position().distanceToSqr(center)<=radius*radius && Allies.areAllies(frame.caster(),target));
        targets.sort(Comparator.comparingDouble((LivingEntity target)->target.position().distanceToSqr(center)).thenComparingInt(Entity::getId));
        CastContexts.bind(context,frame);
        for(var target:targets) {
            if(!target.isAlive() || target.isRemoved()) continue;
            var hit=new EntityHitResult(target,target.getBoundingBox().getCenter());
            var branch=resolver.clone();var scoped=frame.withTarget(hit);
            CastContexts.bind(branch.spellContext,scoped);
            CastContexts.run(scoped,()->branch.onResolveEffect(world,hit));
            ArsAccess.allyVisual(context,world,target);
        }
        return CastResolveType.SUCCESS;
    }
    public CastResolveType onCastOnBlock(UseOnContext use,SpellStats stats,SpellContext context,SpellResolver resolver) {
        return onCast(use.getItemInHand(),use.getPlayer(),use.getLevel(),stats,context,resolver);
    }
    public CastResolveType onCastOnBlock(BlockHitResult hit,LivingEntity caster,SpellStats stats,SpellContext context,SpellResolver resolver) {
        return onCast(ItemStack.EMPTY,caster,caster.level(),stats,context,resolver);
    }
    public CastResolveType onCastOnEntity(ItemStack stack,LivingEntity caster,Entity target,InteractionHand hand,SpellStats stats,SpellContext context,SpellResolver resolver) {
        return onCast(stack,caster,caster.level(),stats,context,resolver);
    }
}
