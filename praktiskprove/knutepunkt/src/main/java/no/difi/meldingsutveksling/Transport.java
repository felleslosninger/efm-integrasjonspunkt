package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.noarkexchange.SendMessageTemplate;
import no.difi.meldingsutveksling.oxalisexchange.MessageReceieverTemplate;

/**
 * A transport class
 */
public abstract class Transport {

    private SendMessageTemplate sender;
    private MessageReceieverTemplate receiver;

    protected Transport() {
    }

    protected Transport(SendMessageTemplate sender, MessageReceieverTemplate receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    public SendMessageTemplate getSender() {
        return sender;
    }

    public void setSender(SendMessageTemplate sender) {
        this.sender = sender;
    }

    public MessageReceieverTemplate getReceiver() {
        return receiver;
    }

    public void setReceiver(MessageReceieverTemplate receiver) {
        this.receiver = receiver;
    }
}
