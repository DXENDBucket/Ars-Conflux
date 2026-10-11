package dev.arsconflux.api.casting;
import java.util.UUID;
/** Independent visual countdown, corrected by server packets. No dependency on a native casting state. */
public class ChantProgressTimer {
    private UUID token;
    private int duration, remaining;
    private long syncedAt;
    public void update(UUID token, int duration, int remaining, long now) {
        if (remaining <= 0) { if (token.equals(this.token)) clear(); return; }
        if (duration <= 0) return;
        this.token = token; this.duration = duration; this.remaining = Math.min(remaining, duration); this.syncedAt = now;
    }
    public void clear() { token = null; duration = 0; remaining = 0; }
    public boolean active(long now) { return token != null && now >= syncedAt && remaining(now) > 0; }
    public int duration() { return duration; }
    public int remaining(long now) { return (int)Math.max(0, remaining - Math.max(0L, now - syncedAt)); }
    public float progress(long now) { return duration == 0 ? 0 : 1f - remaining(now) / (float)duration; }
}
