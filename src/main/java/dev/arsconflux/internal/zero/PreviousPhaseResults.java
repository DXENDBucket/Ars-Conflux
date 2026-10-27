package dev.arsconflux.internal.zero;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Each actual execution reads a fixed snapshot and writes into a separate output buffer. */
public final class PreviousPhaseResults<T> {
    public static final class Step<T> {
        private final List<T> input;
        private final List<T> output = new ArrayList<>();

        private Step(List<T> input) {
            this.input = List.copyOf(input);
        }

        public List<T> input() { return input; }
        public List<T> output() { return List.copyOf(output); }
    }

    private Step<T> current;

    public Step<T> advance() {
        current = new Step<>(current == null ? List.of() : current.output);
        return current;
    }

    public boolean isCurrent(Step<T> step) { return step != null && step == current; }

    public boolean append(Step<T> step, T result) {
        if (!isCurrent(step) || result == null) return false;
        step.output.add(result);
        return true;
    }

    public boolean replace(Step<T> step, Collection<T> results) {
        if (!isCurrent(step)) return false;
        var copy = List.copyOf(results);
        step.output.clear();
        step.output.addAll(copy);
        return true;
    }

    public void reset() { current = null; }
}
