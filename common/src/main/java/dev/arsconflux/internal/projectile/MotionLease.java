package dev.arsconflux.internal.projectile;

/** Temporary holds and gravity suppression share one original gravity snapshot. */
public final class MotionLease {
    private final boolean originalNoGravity;
    private java.lang.ref.WeakReference<Object> holder=new java.lang.ref.WeakReference<>(null);
    private long until;
    private boolean suppressed;
    public MotionLease(boolean originalNoGravity) { this.originalNoGravity=originalNoGravity; }
    public void hold(Object owner,long until) { holder=new java.lang.ref.WeakReference<>(java.util.Objects.requireNonNull(owner)); this.until=until; }
    public void release(Object owner) { if(holder.get()==owner)holder.clear(); }
    public void launch() { holder.clear(); }
    public void suppress(boolean value) { suppressed=value; }
    public boolean suppressed() { return suppressed; }
    public boolean held(long now) { if(now>=until)holder.clear(); return holder.get()!=null; }
    public boolean active(long now) { return held(now) || suppressed; }
    public boolean noGravity(long now) { return active(now) || originalNoGravity; }
}
