package no.difi.meldingsutveksling.core;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;

import static no.difi.meldingsutveksling.core.EDUCore.MessageType.EDU;

/**
 * Internal mapping object for generic handling of e.g. BEST EDU and MXA formats.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EDUCore", propOrder = {
        "id",
        "messageReference",
        "sender",
        "receiver",
        "serviceIdentifier",
        "messageType",
        "payload"
})
public class EDUCore {

    @XmlAttribute(name = "id", required = true)
    private String id;
    @XmlElement
    private String messageReference;
    @XmlElement(required = true)
    private Sender sender;
    @XmlElement(required = true)
    private Receiver receiver;
    @XmlElement
    private ServiceIdentifier serviceIdentifier; // TODO: remove when MessageStrategy is Springified (due to ReceiptRepo..)
    @XmlElement(required = true)
    private MessageType messageType;
    @XmlElement(required = true)
    private Object payload;

    public EDUCore() {}

    @XmlType
    @XmlEnum(Integer.class)
    public enum MessageType {
        @XmlEnumValue("1") EDU,
        @XmlEnumValue("2") APPRECEIPT,
        @XmlEnumValue("3") UNKNOWN
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

    /**
     * From PutMessage (BEST/EDU) this is the conversation id
     *
     * @param id of the message ie. the conversation id
     */
    public void setId(String id) {
        this.id = id;
    }

    public String getMessageReference() {
        return messageReference;
    }

    public void setMessageReference(String messageReference) {
        this.messageReference = messageReference;
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

    public ServiceIdentifier getServiceIdentifier() {
        return serviceIdentifier;
    }

    public void setServiceIdentifier(ServiceIdentifier serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }


    public boolean hasPayload() {
        return this.payload != null;
    }

    public String getJournalpostId() {
        if (getMessageType() == EDU) {
            return ((MeldingType) getPayload()).getJournpost().getJpId();
        }
        return "";
    }

    public MeldingType getPayloadAsMeldingType() {
        return (MeldingType) getPayload();
    }

    public AppReceiptType getPayloadAsAppreceiptType() {
        return (AppReceiptType) getPayload();
    }

    public void swapSenderAndReceiver() {
        Sender sender = new Sender();
        sender.setIdentifier(getReceiver().getIdentifier());
        sender.setName(getReceiver().getName());
        Receiver receiver = new Receiver();
        receiver.setIdentifier(getSender().getIdentifier());
        receiver.setName(getSender().getName());
        setSender(sender);
        setReceiver(receiver);
    }

    public JAXBElement<EDUCore> getAsJaxBElement() {
        return new JAXBElement<>(new QName("uri", "local"), EDUCore.class, this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("messageReference", messageReference)
                .add("sender", sender)
                .add("receiver", receiver)
                .toString();
    }

    @Override
    @SuppressWarnings({"squid:S00122", "squid:S1067"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EDUCore eduCore = (EDUCore) o;
        return Objects.equal(id, eduCore.id) &&
                Objects.equal(messageReference, eduCore.messageReference) &&
                Objects.equal(sender, eduCore.sender) &&
                Objects.equal(receiver, eduCore.receiver) &&
                serviceIdentifier == eduCore.serviceIdentifier &&
                messageType == eduCore.messageType &&
                Objects.equal(payload, eduCore.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, messageReference, sender, receiver, serviceIdentifier, messageType, payload);
    }
}
