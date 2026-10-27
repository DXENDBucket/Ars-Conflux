package dev.arsconflux.internal.zero;

import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.spell.*;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.spell.*;
import dev.arsconflux.config.ConfluxStartupConfig;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.*;

/** Adapts storage boundaries while leaving native spell effects, visuals and resolver order intact. */
public final class ZeroTemporal {
    private static final ResourceLocation KEY=ResourceLocation.fromNamespaceAndPath("ars_conflux","zero_execution");
    public record Binding(ZeroTemporalState owner,PreviousPhaseResults.Step<SpellResult> step,List<SpellResult> scoped) implements IContextAttachment {
        public ResourceLocation id() { return KEY; }
        List<SpellResult> input() { return scoped==null?step.input():scoped; }
    }
    private ZeroTemporal() {}
    public static Binding binding(SpellContext context) {
        return ConfluxStartupConfig.zeroTemporal() ? context.getAttachment(KEY) : null;
    }
    public static void begin(SpellResolver resolver,SpellPhase phase) {
        if(!ConfluxStartupConfig.zeroTemporal())return;
        var caster=IMultiPhaseCaster.from(resolver.spellContext,resolver.spellContext.getUnwrappedCaster());
        var nativeContext=caster==null?null:caster.getCastContext();
        if(!(nativeContext instanceof ZeroTemporalOwner holder))return;
        var state=holder.arsConflux$temporal();
        state.start(phase);
        resolver.spellContext.attachments.put(KEY,new Binding(state,state.step,null));
    }
    private static <T> T expose(ZeroTemporalState state,List<SpellResult> selection,Supplier<T> original) {
        var saved=List.copyOf(state.nativeContext.beginResults);
        try {
            state.nativeContext.beginResults.clear(); state.nativeContext.beginResults.addAll(selection);
            return original.get();
        } finally {
            state.nativeContext.beginResults.clear(); state.nativeContext.beginResults.addAll(saved);
        }
    }
    public static CastResolveType resolve(SpellContext context,Supplier<CastResolveType> original) {
        var binding=binding(context);
        if(binding==null)return original.get();
        if(!binding.owner().accepts(binding))return CastResolveType.FAILURE;
        var input=binding.input().stream().filter(ZeroTemporalState::valid)
                .filter(r -> r.targetEntity==null || r.targetEntity.level()==context.level).toList();
        try { return expose(binding.owner(),input,original); }
        finally { binding.owner().sync(); }
    }
    public static void target(SpellResolver child,HitResult hit,Runnable original) {
        var binding=binding(child.spellContext);
        if(binding==null) { original.run(); return; }
        var selected=binding.input().stream().filter(r -> hit instanceof EntityHitResult entity
                ? r.targetEntity==entity.getEntity() : hit instanceof BlockHitResult block && block.getBlockPos().equals(r.targetPosition)).toList();
        child.spellContext.attachments.put(KEY,new Binding(binding.owner(),binding.step(),selected));
        original.run();
    }
    /** Producer helpers all retain their original despawn/lifespan behavior. */
    public static void produced(SpellContext context,Runnable original) {
        var binding=binding(context);
        if(binding==null) { original.run(); return; }
        expose(binding.owner(),binding.input(),() -> {
            original.run();
            binding.owner().record(binding,List.copyOf(binding.owner().nativeContext.beginResults),true);
            return null;
        });
        if(binding.owner().closed)AnchorEffect.restoreEntityPhysics(binding.owner().nativeContext);
        binding.owner().sync();
    }
    public static void producedEntity(SpellContext context,net.minecraft.world.entity.Entity entity,Runnable original) {
        var binding=binding(context);
        if(binding==null) { original.run(); return; }
        // Native Convergence writes a phase-specific list. Preserve that native work, but bound its history.
        var owner=binding.owner(); var nativeContext=owner.nativeContext;
        var tick=List.copyOf(nativeContext.tickResults); var end=List.copyOf(nativeContext.endResults);
        expose(owner,binding.input(),() -> { original.run(); return null; });
        nativeContext.tickResults.clear(); nativeContext.tickResults.addAll(tick);
        nativeContext.endResults.clear(); nativeContext.endResults.addAll(end);
        var result=SpellResult.fromHitResultWithCaster(new EntityHitResult(entity),SpellEffectType.RESOLVED,context.getCaster());
        owner.record(binding,List.of(result),true);
    }
    public static void anchor(SpellContext context,Runnable original) {
        var binding=binding(context);
        if(binding==null) { original.run(); return; }
        if(!binding.owner().accepts(binding))return;
        for(var result:binding.input())binding.owner().remember(result);
        expose(binding.owner(),binding.input(),() -> { original.run(); return null; });
        binding.owner().record(binding,binding.input(),true);
    }
    public static boolean stale(SpellContext context) {
        var binding=binding(context); return binding!=null && !binding.owner().accepts(binding);
    }
    public static void observed(EffectResolveEvent.Post event,Runnable original) {
        var binding=binding(event.resolver.spellContext);
        if(binding==null) { original.run(); return; }
        if(!binding.owner().accepts(binding))return;
        var owner=binding.owner();
        var phase=WrappedSpellResolver.extractPhase(event.resolver,owner.nativeContext);
        if(phase==SpellPhase.BEGIN) {
            expose(owner,owner.step.output(),() -> {
                original.run();
                var results=List.copyOf(owner.nativeContext.beginResults);
                boolean hasGroup=results.stream().anyMatch(r -> r!=null && r.blockGroup!=null);
                owner.record(binding,results,hasGroup);
                return null;
            });
        } else {
            var hit=SpellResult.fromHitResultWithCaster(event.rayTraceResult,SpellEffectType.RESOLVED,event.resolver.spellContext.getCaster());
            if(hit!=null)owner.record(binding,List.of(hit),false);
        }
        owner.sync();
    }
    public static void restore(MultiPhaseCastContext context,java.util.function.Consumer<MultiPhaseCastContext> original) {
        if(ConfluxStartupConfig.zeroTemporal() && context instanceof ZeroTemporalOwner holder && holder.arsConflux$temporal().step!=null)
            holder.arsConflux$temporal().restore(original);
        else original.accept(context);
    }
    public static void close(MultiPhaseCastContext context) {
        if(context instanceof ZeroTemporalOwner holder) {
            AnchorEffect.restoreEntityPhysics(context);
            holder.arsConflux$temporal().close();
        }
    }
    public static List<SpellResult> physicsResults(MultiPhaseCastContext context) {
        return ConfluxStartupConfig.zeroTemporal() && context instanceof ZeroTemporalOwner holder
                && holder.arsConflux$temporal().step!=null ? holder.arsConflux$temporal().physicsResults() : context.beginResults;
    }
}
