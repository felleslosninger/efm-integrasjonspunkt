package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

/**
 * Strategy for managing Best/EDU messages on receive
 *
 * @author Glenn Bech
 */
class BestEDUMessageStrategy implements MessageStrategy {

    private MessageSender messageSender;

    BestEDUMessageStrategy(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public PutMessageResponseType send(EDUCore request) {
        request.setServiceIdentifier(ServiceIdentifier.DPO);
        return messageSender.sendMessage(request);
    }
}
