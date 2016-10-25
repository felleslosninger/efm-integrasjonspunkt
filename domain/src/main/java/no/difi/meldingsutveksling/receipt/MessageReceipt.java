package no.difi.meldingsutveksling.receipt;

import com.google.common.base.MoreObjects;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * Used for storing and tracking receipt information.
 */
@Entity
public class MessageReceipt {

    @Id
    private String messageId;
    private String messageReference;
    private String messageTitle;
    private LocalDateTime lastUpdate;
    private ServiceIdentifier targetType;
    private boolean received;

    MessageReceipt(){}

    private MessageReceipt(String id, String msgRef, String msgTitle, ServiceIdentifier type) {
        this.messageId = id;
        this.messageReference = msgRef;
        this.messageTitle = msgTitle;
        this.lastUpdate = LocalDateTime.now();
        this.targetType = type;
        this.received = false;
    }

    public static MessageReceipt of(String id, String msgRef, String msgTitle, ServiceIdentifier type) {
        return new MessageReceipt(id, msgRef, msgTitle, type);
    }

    public static MessageReceipt of(EDUCore eduCore) {
        if (eduCore.getServiceIdentifier() == null) {
            throw new IllegalArgumentException("ServiceIdentifier not set on EDUCore.");
        }

        String msgTitle = "";
        if (eduCore.getMessageType() == EDUCore.MessageType.EDU) {
            msgTitle = eduCore.getPayloadAsMeldingType().getJournpost().getJpInnhold();
        }

        return new MessageReceipt(eduCore.getId(),
                eduCore.getMessageReference(),
                msgTitle,
                eduCore.getServiceIdentifier());
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageReference() {
        return messageReference;
    }

    public void setMessageReference(String messageReference) {
        this.messageReference = messageReference;
    }

    public String getMessageTitle() {
        return messageTitle;
    }

    public void setMessageTitle(String messageTitle) {
        this.messageTitle = messageTitle;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public ServiceIdentifier getTargetType() {
        return targetType;
    }

    public void setTargetType(ServiceIdentifier targetType) {
        this.targetType = targetType;
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("messageId", messageId)
                .add("messageReference", messageReference)
                .add("messageTitle", messageTitle)
                .add("lastUpdate", lastUpdate)
                .add("targetType", targetType)
                .add("received", received)
                .toString();
    }
}
