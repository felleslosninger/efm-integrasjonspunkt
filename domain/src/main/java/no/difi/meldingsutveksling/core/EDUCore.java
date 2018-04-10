package no.difi.meldingsutveksling.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;

import javax.xml.bind.annotation.*;

/**
 * Internal mapping object for generic handling of e.g. BEST EDU and MXA formats.
 */
@Slf4j
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
@Data
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

    public String getJournalpostId() {
        if (this.getMessageType() == MessageType.APPRECEIPT) {
            return "";
        }
        return PayloadUtil.queryJpId(this.payload);
    }

    public void swapSenderAndReceiver() {
        Sender sender = Sender.of(getReceiver().getIdentifier(), getReceiver().getName(), getReceiver().getRef());
        Receiver receiver = Receiver.of(getSender().getIdentifier(), getSender().getName(), getSender().getRef());

        setSender(sender);
        setReceiver(receiver);
    }

}
