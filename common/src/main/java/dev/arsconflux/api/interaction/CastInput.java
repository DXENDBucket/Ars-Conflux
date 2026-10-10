package dev.arsconflux.api.interaction;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Objects;

/** Server-side input snapshot. A hit may be absent: the provider can select using origin/direction.
 * Non-player sources may submit the same input directly; this API does not simulate mouse clicks. */
public record CastInput(Action action, InteractionHand hand, Vec3 origin, Vec3 direction, HitResult hit) {
    public enum Action { ACTIVATE, CONFIRM, CANCEL }
    public CastInput {
        Objects.requireNonNull(action); Objects.requireNonNull(origin); Objects.requireNonNull(direction);
        direction = direction.normalize();
    }
}
