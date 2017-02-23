package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.ks.Forsendelse;

class ForsendelseHandler implements Handler<no.difi.meldingsutveksling.ks.Forsendelse.Builder> {

    @Override
    public Forsendelse.Builder map(Forsendelse.Builder builder) {
        return builder.withKunDigitalLevering(true);
    }
}
