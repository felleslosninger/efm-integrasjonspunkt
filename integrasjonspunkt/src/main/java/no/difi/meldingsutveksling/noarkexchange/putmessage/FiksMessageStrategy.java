package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.SvarUtService;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

class FiksMessageStrategy implements MessageStrategy {
    private SvarUtService svarUtService;

    FiksMessageStrategy(SvarUtService svarUtService) {

        this.svarUtService = svarUtService;
    }

    @Override
    public PutMessageResponseType send(EDUCore request) {
        final String send = svarUtService.send(request); // TODO return value
        return PutMessageResponseFactory.createOkResponse();
    }
}
