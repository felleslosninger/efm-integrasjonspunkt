package no.difi.meldingsutveksling.ks.mapping;

@FunctionalInterface
public interface Handler<T> {

        T map(T builder);
}
