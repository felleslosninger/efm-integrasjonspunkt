package no.difi.meldingsutveksling.receipt;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import no.difi.meldingsutveksling.nextmove.AbstractEntity;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 * Used for storing and tracking receipt information.
 */
@Getter
@Setter
@Entity
@Table(name = "message_status")
@NamedEntityGraph(name = "MessageStatus.conversation", attributeNodes = @NamedAttributeNode("conversation"))
public class MessageStatus extends AbstractEntity<Long> {

    @Override
    @JsonProperty
    @ApiModelProperty(
            position = 2,
            value = "Id",
            example = "1")
    public Long getId() {
        return super.getId();
    }

    @JsonProperty
    public Long getConvId() {
        return conversation.getId();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conv_id")
    private Conversation conversation;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime lastUpdate;
    private String status;
    private String description;

    @Lob
    private String rawReceipt;

    MessageStatus() {
    }

    public String getConversationId() {
        return conversation.getConversationId();
    }

    private MessageStatus(String status, OffsetDateTime lastUpdate, String description) {
        this.status = status;
        this.lastUpdate = lastUpdate;
        this.description = description;
    }

    @JsonIgnore
    public Conversation getConversation() {
        return conversation;
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
                .add("id", getId())
                .add("lastUpdate", lastUpdate)
                .add("status", status)
                .add("description", description)
                .add("rawReceipt", rawReceipt)
                .toString();
    }
}
