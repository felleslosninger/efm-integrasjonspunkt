package no.difi.meldingsutveksling.cucumber;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class Holder<T> {

    private final AtomicReference<T> t = new AtomicReference<>();

    T get() {
        return t.get();
    }

    void set(T in) {
        t.set(in);
    }

    boolean isPresent() {
        return t.get() != null;
    }

    void reset() {
        t.set(null);
    }

    T getOrCalculate(Supplier<T> supplier) {
        if (!isPresent()) {
            t.set(supplier.get());
        }

        return get();
    }
}
