package dev.arsconflux.api.projectile;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;

/** Implement on a visible replacement entity to exclude its own off-world carrier's queries. */
public interface CarrierProxy {
    EntityProjectileSpell confluxTrajectory();
}
