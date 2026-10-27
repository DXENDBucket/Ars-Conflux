package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.common.entity.EntityHomingProjectileSpell;
import dev.arsconflux.api.projectile.ReflectableCarrier;
import dev.arsconflux.api.targeting.Allies;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;

/** A reflected homing projectile must release its previous target and owner-bound ignore closures. */
@Mixin(value=EntityHomingProjectileSpell.class,remap=false)
public abstract class HomingReflectionMixin implements ReflectableCarrier {
    @Shadow LivingEntity target;
    @Unique private List<Predicate<LivingEntity>> conflux$originalIgnored;
    @Unique private LivingEntity conflux$originalOwner;
    public boolean canReflect() {
        try {
            return getClass().getMethod("tickNextPosition").getDeclaringClass()==EntityHomingProjectileSpell.class
                    && getClass().getMethod("getNextHitPosition").getDeclaringClass()==com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell.class;
        } catch(ReflectiveOperationException absent) { return false; }
    }
    public void reflected(LivingEntity previousOwner,LivingEntity owner) {
        target=null;
        if(owner==null)return;
        var projectile=(EntityHomingProjectileSpell)(Object)this;
        if(conflux$originalIgnored==null) {
            conflux$originalIgnored=projectile.getIgnored()==null?List.of():List.copyOf(projectile.getIgnored());
            conflux$originalOwner=previousOwner;
        }
        // Keep targeting restrictions for other entities; reflecting back at the original shooter is allowed.
        // Repeated reflection reuses the original predicates rather than nesting owner closures indefinitely.
        projectile.setIgnored(List.of(entity->Allies.areAllies(owner,entity)
                || entity!=conflux$originalOwner && conflux$originalIgnored.stream().anyMatch(ignore->ignore.test(entity))));
    }
}
