package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;
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
        request.setServiceIdentifier(ServiceIdentifier.FIKS);
        svarUtService.send(request);
        return PutMessageResponseFactory.createOkResponse();
    }
}
