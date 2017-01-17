package no.difi.meldingsutveksling.ks.mapping.properties;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.Forsendelse;

/**
 * Used to map needed properties for Svar Ut web service to Forsendelse
 */
public class SvarUtConfigHandler extends PropertyHandler<Forsendelse.Builder> {
    public SvarUtConfigHandler(IntegrasjonspunktProperties properties) {
        super(properties);
    }

    @Override
    public Forsendelse.Builder map(Forsendelse.Builder builder) {
        builder.withKonteringskode(getProperties().getDps().getKonverteringsKode());
        builder.withKryptert(getProperties().getDps().isKryptert());
        return builder;
    }
}
