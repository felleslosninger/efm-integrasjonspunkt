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
    private LocalDateTime lastUpdate;
    private ServiceIdentifier targetType;
    private boolean received;

    MessageReceipt(){}

    private MessageReceipt(String id, ServiceIdentifier type) {
        this.messageId = id;
        this.lastUpdate = LocalDateTime.now();
        this.targetType = type;
        this.received = false;
    }

    public static MessageReceipt of(String id, ServiceIdentifier type) {
        return new MessageReceipt(id, type);
    }

    public static MessageReceipt of(EDUCore eduCore) {
        if (eduCore.getServiceIdentifier() == null) {
            throw new IllegalArgumentException("ServiceIdentifier not set on EDUCore.");
        }
        return new MessageReceipt(eduCore.getId(), eduCore.getServiceIdentifier());
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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
                .add("targetType", targetType)
                .toString();
    }

}
