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
    private String id;

    private LocalDateTime lastUpdate;
    private ReceiptStatus status;

    @Lob
    private String rawReceipt;

    MessageReceipt(){}

    private MessageReceipt(ReceiptStatus status, LocalDateTime lastUpdate) {
        this.status = status;
        this.lastUpdate = lastUpdate;
    }

    public static MessageReceipt of(ReceiptStatus status, LocalDateTime lastUpdate) {
        return new MessageReceipt(status, lastUpdate);
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("lastUpdate", lastUpdate)
                .add("status", status)
                .add("rawReceipt", rawReceipt)
                .toString();
    }
}
