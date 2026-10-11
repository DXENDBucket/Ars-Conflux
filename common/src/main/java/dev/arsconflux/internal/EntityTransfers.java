package dev.arsconflux.internal;

import dev.arsconflux.mixin.EntityWorldAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.Vec3;

/** Preserve transient provider state: managed projectiles are moved, never serialized and cloned. */
public final class EntityTransfers {
    private EntityTransfers() {}
    public static void carrier(Entity entity, ServerLevel destination) {
        ((EntityWorldAccess)entity).arsConflux$level(destination);
    }
    public static boolean projectile(Entity entity, ServerLevel destination, Vec3 position) {
        if (entity.isRemoved()) return false;
        if (entity.level() == destination) { entity.setPos(position); return true; }
        entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
        entity.setLevelCallback(EntityInLevelCallback.NULL);
        entity.revive();
        carrier(entity, destination);
        entity.setPos(position);
        boolean spawned = destination.addFreshEntity(entity);
        if (!spawned) entity.discard();
        return spawned;
    }
}
