package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;

import java.io.IOException;

public class OxalisSendMessageTemplate extends SendMessageTemplate {


    @Override
    SBD createSBD(PutMessageRequestType sender) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void sendSBD(SBD sbd) throws IOException {

    }
}
