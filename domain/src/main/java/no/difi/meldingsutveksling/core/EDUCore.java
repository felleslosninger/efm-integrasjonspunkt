package no.difi.meldingsutveksling.core;

import com.google.common.base.MoreObjects;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;

/**
 * Internal mapping object for generic handling of e.g. BEST EDU and MXA formats.
 */
public class EDUCore {

    private String id;
    private Sender sender;
    private Receiver receiver;
    private Object payload;
    private MessageType messageType;

    EDUCore() {}

    public enum MessageType {
        EDU,
        MXA,
        APPRECEIPT,
        UNKNOWN
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Sender getSender() {
        return sender;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public Receiver getReceiver() {
        return receiver;
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("sender", sender)
                .add("receiver", receiver)
                .toString();
    }
}
