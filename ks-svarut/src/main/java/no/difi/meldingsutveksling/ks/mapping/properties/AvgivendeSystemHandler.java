package no.difi.meldingsutveksling.ks.mapping.properties;

import com.google.common.base.Objects;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.Forsendelse;

public class AvgivendeSystemHandler extends PropertyHandler<Forsendelse.Builder> {

    public AvgivendeSystemHandler(IntegrasjonspunktProperties properties) {
        super(properties);
    }

    @Override
    public Forsendelse.Builder map(Forsendelse.Builder builder) {
        builder.withAvgivendeSystem(super.getProperties().getNoarkSystem().getType());
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AvgivendeSystemHandler)) return false;
        AvgivendeSystemHandler that = (AvgivendeSystemHandler) o;
        return Objects.equal(super.getProperties(), that.getProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.getProperties());
    }
}
