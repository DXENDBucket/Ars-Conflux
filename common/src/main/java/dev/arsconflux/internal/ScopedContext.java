package dev.arsconflux.internal;

/** Null inherits the outer scope; an explicit empty value masks it. */
public final class ScopedContext<T> {
    private final ThreadLocal<T> current=new ThreadLocal<>();
    public T current() { return current.get(); }
    public void run(T value,Runnable action) {
        var previous=current.get();
        if(value!=null)current.set(value);
        try { action.run(); }
        finally { if(previous==null)current.remove(); else current.set(previous); }
    }
}
