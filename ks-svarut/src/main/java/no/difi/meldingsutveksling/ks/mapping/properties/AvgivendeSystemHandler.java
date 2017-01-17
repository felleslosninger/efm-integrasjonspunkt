package no.difi.meldingsutveksling.ks.mapping.properties;

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


}
