package dev.arsconflux.internal.projectile;

/** Native entity metadata, shared with clients; bit 1 holds motion, bit 2 suppresses gravity. */
public interface MotionControlData {
    int conflux$motionFlags();
    void conflux$motionFlags(int flags);
}
