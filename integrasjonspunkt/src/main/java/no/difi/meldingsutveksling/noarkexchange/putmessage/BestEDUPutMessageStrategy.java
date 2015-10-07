package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

/**
 * Strategy for managing Best/EDU messages on receive
 *
 * @author Glenn Bech
 */
class BestEDUPutMessageStrategy implements PutMessageStrategy {

    private MessageSender messageSender;

    public BestEDUPutMessageStrategy(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType requestType) {
        return messageSender.sendMessage(requestType);
    }
}
