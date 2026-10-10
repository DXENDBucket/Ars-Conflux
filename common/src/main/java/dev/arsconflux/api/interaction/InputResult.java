package dev.arsconflux.api.interaction;

/** REJECTED consumes the operation but changes no stage; PASS permits an ordinary cast. */
public enum InputResult {
    PASS, REJECTED, ACCEPTED, COMPLETED;
    public boolean consumed() { return this != PASS; }
}
