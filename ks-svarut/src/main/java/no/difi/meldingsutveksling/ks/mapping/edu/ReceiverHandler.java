package no.difi.meldingsutveksling.ks.mapping.edu;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.Forsendelse;
import no.difi.meldingsutveksling.ks.Organisasjon;
import no.difi.meldingsutveksling.ks.mapping.Handler;

public class ReceiverHandler implements Handler<Forsendelse.Builder> {
    private EDUCore eduCore;

    public ReceiverHandler(EDUCore eduCore) {
        this.eduCore = eduCore;
    }

    @Override
    public Forsendelse.Builder map(Forsendelse.Builder builder) {
        final Organisasjon mottaker = new Organisasjon();
        mottaker.setOrgnr(eduCore.getReceiver().getIdentifier());
        mottaker.setPostnr("0192");
        mottaker.setPoststed("Oslo");
        mottaker.setNavn("Navn");
        builder.withMottaker(mottaker);
        return builder;
    }
}
