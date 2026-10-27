package dev.arsconflux.internal.zero;

import com.github.ars_zero.common.spell.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

/** Separate fixed input and current output; old asynchronous callbacks cannot change selection. */
public final class ZeroTemporalState {
    final MultiPhaseCastContext nativeContext;
    final PreviousPhaseResults<SpellResult> history = new PreviousPhaseResults<>();
    PreviousPhaseResults.Step<SpellResult> step;
    SpellPhase phase;
    boolean produced, closed;
    private final Map<UUID, Physics> cleanup = new LinkedHashMap<>();
    private record Physics(SpellResult result, boolean noGravity, boolean noPhysics) {}
    public ZeroTemporalState(MultiPhaseCastContext context) { nativeContext=context; }
    void start(SpellPhase next) {
        step=history.advance(); phase=next; produced=false;
        sync();
    }
    boolean accepts(ZeroTemporal.Binding binding) { return !closed && binding!=null && binding.owner()==this && history.isCurrent(binding.step()); }
    void record(ZeroTemporal.Binding binding, Collection<SpellResult> results, boolean generated) {
        // Even a late-created block group needs native block restoration at the end of the use.
        for(var result:results) if(result!=null && result.blockGroup!=null) remember(result);
        if(!accepts(binding) || produced && !generated)return;
        if(generated && !produced) { history.replace(step,List.of()); produced=true; }
        for(var result:results) {
            if(!valid(result))continue;
            boolean duplicate=step.output().stream().anyMatch(old -> sameTarget(old,result));
            if(!duplicate)history.append(step,result);
        }
        sync();
    }
    static boolean sameTarget(SpellResult a,SpellResult b) {
        return a.targetEntity!=null ? a.targetEntity==b.targetEntity : a.targetPosition!=null && a.targetPosition.equals(b.targetPosition);
    }
    static boolean valid(SpellResult result) {
        return result!=null && result.hitResult!=null && result.hitResult.getType()!=HitResult.Type.MISS
                && (result.targetEntity==null || result.targetEntity.isAlive() && !result.targetEntity.isRemoved());
    }
    void remember(SpellResult result) {
        Entity entity=result.targetEntity;
        if(entity!=null)cleanup.putIfAbsent(entity.getUUID(),new Physics(result,entity.isNoGravity(),entity.noPhysics));
    }
    List<SpellResult> physicsResults() { return cleanup.values().stream().map(Physics::result).toList(); }
    void sync() {
        if(step==null)return;
        var output=step.output();
        nativeContext.beginResults.clear(); nativeContext.beginResults.addAll(output);
        var list=phase==SpellPhase.TICK?nativeContext.tickResults:phase==SpellPhase.END?nativeContext.endResults:null;
        if(list!=null) { list.clear(); list.addAll(output); }
    }
    /** Run native placement/cleanup for all touched objects, forwarding only the latest selection. */
    void restore(Consumer<MultiPhaseCastContext> nativeRestore) {
        if(cleanup.isEmpty())return;
        var selected=new ArrayList<>(step==null?List.<SpellResult>of():step.output());
        var tracked=List.copyOf(cleanup.values()); cleanup.clear();
        for(var state:tracked) {
            var result=state.result(); var entity=result.targetEntity;
            var saved=List.copyOf(nativeContext.beginResults);
            List<SpellResult> restored;
            try {
                nativeContext.beginResults.clear(); nativeContext.beginResults.add(result);
                nativeRestore.accept(nativeContext);
                restored=List.copyOf(nativeContext.beginResults);
            } finally {
                if(entity!=null && result.blockGroup==null && !entity.isRemoved()) {
                    entity.setNoGravity(state.noGravity()); entity.noPhysics=state.noPhysics();
                }
                nativeContext.beginResults.clear(); nativeContext.beginResults.addAll(saved);
            }
            boolean wasSelected=selected.removeIf(old -> sameTarget(old,result));
            if(wasSelected)for(var item:restored)if(valid(item))selected.add(item);
        }
        if(step!=null)history.replace(step,selected);
        sync();
    }
    void close() { closed=true; }
}
