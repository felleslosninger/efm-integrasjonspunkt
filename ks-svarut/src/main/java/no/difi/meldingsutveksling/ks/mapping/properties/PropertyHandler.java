package no.difi.meldingsutveksling.ks.mapping.properties;

import com.google.common.base.Objects;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.Handler;

public class PropertyHandler<T> implements Handler<T> {
    private final IntegrasjonspunktProperties properties;

    public PropertyHandler(IntegrasjonspunktProperties properties) {
        this.properties = properties;
    }

    @Override
    public T map(T builder) {
        return builder;
    }

    public IntegrasjonspunktProperties getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyHandler)) return false;
        PropertyHandler<?> that = (PropertyHandler<?>) o;
        return Objects.equal(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(properties);
    }
}
