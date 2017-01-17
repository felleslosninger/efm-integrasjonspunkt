package no.difi.meldingsutveksling.receipt;

import com.google.common.base.MoreObjects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import java.time.LocalDateTime;

/**
 * Used for storing and tracking receipt information.
 */
@Entity
public class MessageReceipt {

    @Id
    @GeneratedValue
    private Integer genId;

    private LocalDateTime lastUpdate;
    private ReceiptStatus status;
    private String description;

    @Lob
    private String rawReceipt;

    MessageReceipt(){}

    private MessageReceipt(ReceiptStatus status, LocalDateTime lastUpdate, String description) {
        this.status = status;
        this.lastUpdate = lastUpdate;
        this.description = description;
    }

    public static MessageReceipt of(ReceiptStatus status, LocalDateTime lastUpdate) {
        return new MessageReceipt(status, lastUpdate, null);
    }

    public static MessageReceipt of(ReceiptStatus status, LocalDateTime lastUpdate, String description) {
        return new MessageReceipt(status, lastUpdate, description);
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getRawReceipt() {
        return rawReceipt;
    }

    public void setRawReceipt(String rawReceipt) {
        this.rawReceipt = rawReceipt;
    }

    public ReceiptStatus getStatus() {
        return status;
    }

    public void setStatus(ReceiptStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("genId", genId)
                .add("lastUpdate", lastUpdate)
                .add("status", status)
                .add("description", description)
                .add("rawReceipt", rawReceipt)
                .toString();
    }
}
