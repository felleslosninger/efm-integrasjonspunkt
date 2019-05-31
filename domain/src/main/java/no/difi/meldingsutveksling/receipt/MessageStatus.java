package no.difi.meldingsutveksling.receipt;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.base.MoreObjects;
import lombok.Data;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 * Used for storing and tracking receipt information.
 */
@Entity
@Data
public class MessageStatus {

    @Id
    @GeneratedValue
    private Integer statId;
    @Column(insertable = false, updatable = false, name = "conv_id")
    private Integer convId;
    private String conversationId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime lastUpdate;
    private String status;
    private String description;

    @Lob
    private String rawReceipt;

    MessageStatus() {
    }

    private MessageStatus(String status, OffsetDateTime lastUpdate, String description) {
        this.status = status;
        this.lastUpdate = lastUpdate;
        this.description = description;
    }

    public static MessageStatus of(ReceiptStatus status, OffsetDateTime lastUpdate) {
        return new MessageStatus(status.toString(), lastUpdate, null);
    }

    public static MessageStatus of(ReceiptStatus status, OffsetDateTime lastUpdate, String description) {
        return new MessageStatus(status.toString(), lastUpdate, description);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("statId", statId)
                .add("lastUpdate", lastUpdate)
                .add("status", status)
                .add("description", description)
                .add("rawReceipt", rawReceipt)
                .toString();
    }
}
