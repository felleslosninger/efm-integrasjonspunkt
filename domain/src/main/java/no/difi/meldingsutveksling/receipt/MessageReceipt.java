package no.difi.meldingsutveksling.receipt;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.base.MoreObjects;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Used for storing and tracking receipt information.
 */
@Entity
@Data
public class MessageReceipt {

    @Id
    @GeneratedValue
    private Integer recId;
    @Column(insertable = false, updatable = false, name = "conv_id")
    private Integer convId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("recId", recId)
                .add("lastUpdate", lastUpdate)
                .add("status", status)
                .add("description", description)
                .add("rawReceipt", rawReceipt)
                .toString();
    }
}
