package dev.arsconflux.api.casting;

/** A channel admits many dispatches but acknowledges its activation only once. */
public final class ActiveUseCommit {
    private boolean acknowledged, closed;
    public boolean open() { return !closed; }
    public void close() { closed = true; }
    public void acknowledge(boolean success, Runnable commit) {
        if (closed || acknowledged || !success) return;
        acknowledged = true;
        commit.run();
    }
}
