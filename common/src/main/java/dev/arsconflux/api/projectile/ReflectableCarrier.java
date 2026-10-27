package dev.arsconflux.api.projectile;

import net.minecraft.world.entity.LivingEntity;

/** Opt in carriers with extra targeting state; positions/velocity/owner are updated by Core. */
public interface ReflectableCarrier {
    default boolean canReflect() { return true; }
    void reflected(LivingEntity previousOwner,LivingEntity reflector);
}
