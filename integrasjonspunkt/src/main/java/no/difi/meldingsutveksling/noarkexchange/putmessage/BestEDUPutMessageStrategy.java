package no.difi.meldingsutveksling.noarkexchange.putmessage;

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

    public BestEDUMessageStrategy(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public PutMessageResponseType putMessage(EDUCore request) {
        return messageSender.sendMessage(request);
    }
}
