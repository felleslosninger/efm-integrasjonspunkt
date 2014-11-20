package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.SBD;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public abstract class SendMessageTemplate {

    @Autowired
    EventLog eventLog;

    SBD createSBD(PutMessageRequestType sender) {
        return new SBD();
    }

    abstract void sendSBD(SBD sbd) throws IOException;

    boolean verifySender(AddressType sender) {
        return true;
    }

    boolean verifyRecipient(AddressType sender) {
        return true;
    }

    public PutMessageResponseType sendMessage(PutMessageRequestType message) {

        verifySender(message.getEnvelope().getSender());
        eventLog.log(new Event());

        verifyRecipient(message.getEnvelope().getSender());
        eventLog.log(new Event());

        SBD sbd = createSBD(message);
        eventLog.log(new Event());

        try {
            sendSBD(sbd);
        } catch (IOException e) {
            eventLog.log(new Event());
        }
        eventLog.log(new Event());
        return new PutMessageResponseType();
    }

}
